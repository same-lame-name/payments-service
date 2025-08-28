package dexter.banking.booktransfers.core.usecase.payment;

import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.PaymentCommand;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.port.*;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The Application Service for the V1 procedural flow.
 * Its role is to orchestrate the use case:
 * 1. Call external services via ports.
 * 2. Based on the outcome, determine the correct next state.
 * 3. Command the aggregate to record the data and transition to the new state.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmitPaymentV1CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    private final CreditCardPort creditCardPort;
    private final DepositPort depositPort;
    private final LimitPort limitPort;
    private final PaymentRepositoryPort paymentRepositoryPort;

    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V1;
    }

    @Override
    public PaymentResult handle(PaymentCommand command) {
        log.info("▶️ [V1] Starting procedural transaction for Command: {}", command.getTransactionReference());

        Payment payment = Payment.startNew(command, UUID::randomUUID);
        paymentRepositoryPort.save(payment);

        try {
            // Step 1: Limit Earmark
            executeLimitEarmark(payment, command);

            // Step 2: Debit Leg
            executeDebitLeg(payment, command);

            // Step 3: Credit Leg
            executeCreditLeg(payment, command);

            // Final success state
            payment.setState(TransactionState.TRANSACTION_SUCCESSFUL);
            paymentRepositoryPort.update(payment);

        } catch (StepFailedException e) {
            log.error("❌ [V1] Procedural transaction FAILED at state '{}' for TXN_ID: {}. Initiating SAGA compensation...",
                    payment.getState(), payment.getTransactionId(), e);

            // Reload the last persisted state to ensure we compensate from the correct point
            Payment lastSavedPayment = paymentRepositoryPort.findById(payment.getTransactionId())
                    .orElseThrow(() -> new IllegalStateException("Payment disappeared during failed transaction: " + payment.getTransactionId()));

            // Manual Saga Compensation
            compensate(lastSavedPayment, command);
        }

        Payment finalPaymentState = paymentRepositoryPort.findById(payment.getTransactionId()).orElseThrow();
        log.info("🏁 [V1] Procedural transaction finished for TXN_ID: {}. Final state: {}",
                finalPaymentState.getTransactionId(), finalPaymentState.getState());
        return PaymentResult.from(finalPaymentState);
    }

    private void executeLimitEarmark(Payment payment, PaymentCommand command) throws StepFailedException {
        var limitRequest = LimitManagementRequest.builder().transactionId(payment.getTransactionId()).limitType(command.getLimitType()).build();
        LimitManagementResponse limitResponse = limitPort.earmarkLimit(limitRequest);
        payment.recordLimitEarmarkResult(limitResponse);

        if (limitResponse.getStatus() == LimitEarmarkStatus.SUCCESSFUL) {
            payment.setState(TransactionState.LIMIT_EARMARK_COMPLETED);
        } else {
            payment.setState(TransactionState.TRANSACTION_FAILED);
            paymentRepositoryPort.update(payment);
            throw new StepFailedException("Limit Earmark Failed");
        }
        paymentRepositoryPort.update(payment);
    }

    private void executeDebitLeg(Payment payment, PaymentCommand command) throws StepFailedException {
        var depositRequest = DepositBankingRequest.builder().transactionId(payment.getTransactionId()).accountNumber(command.getAccountNumber()).build();
        DepositBankingResponse depositResponse = depositPort.submitDeposit(depositRequest);
        payment.recordDebitResult(depositResponse);

        if (depositResponse.getStatus() == DepositBankingStatus.SUCCESSFUL) {
            payment.setState(TransactionState.DEBIT_LEG_COMPLETED);
        } else {
            payment.setState(TransactionState.DEBIT_LEG_FAILED);
            paymentRepositoryPort.update(payment);
            throw new StepFailedException("Deposit (Debit) Failed");
        }
        paymentRepositoryPort.update(payment);
    }

    private void executeCreditLeg(Payment payment, PaymentCommand command) throws StepFailedException {
        var creditRequest = CreditCardBankingRequest.builder().transactionId(payment.getTransactionId()).cardNumber(command.getCardNumber()).build();
        CreditCardBankingResponse creditResponse = creditCardPort.submitCreditCardPayment(creditRequest);
        payment.recordCreditResult(creditResponse);

        if (creditResponse.getStatus() != CreditCardBankingStatus.SUCCESSFUL) {
            payment.setState(TransactionState.CREDIT_LEG_FAILED);
            paymentRepositoryPort.update(payment);
            throw new StepFailedException("Credit Card payment Failed");
        }
        paymentRepositoryPort.update(payment);
    }

    private void compensate(Payment payment, PaymentCommand command) {
        switch (payment.getState()) {
            case CREDIT_LEG_FAILED:
                compensateDebitLeg(payment, command);
                break;
            case DEBIT_LEG_FAILED:
                compensateLimitEarmark(payment, command);
                break;
            default:
                log.warn("  [COMPENSATION] Failure occurred at initial state {}. Nothing to compensate.", payment.getState());
                payment.setState(TransactionState.TRANSACTION_FAILED);
                paymentRepositoryPort.update(payment);
        }
    }

    private void compensateDebitLeg(Payment payment, PaymentCommand command) {
        log.warn("  [COMPENSATION] Reversing Debit Leg for TXN_ID: {}...", payment.getTransactionId());
        var reversalRequest = DepositBankingReversalRequest.builder()
                .transactionId(payment.getTransactionId())
                .reservationId(payment.getDepositBankingResponse().getDepositId())
                .build();
        var reversalResponse = depositPort.submitDepositReversal(payment.getDepositBankingResponse().getDepositId(), reversalRequest);
        payment.recordDebitReversalResult(reversalResponse);

        if (reversalResponse.getStatus() != DepositBankingStatus.REVERSAL_SUCCESSFUL) {
            payment.setState(TransactionState.MANUAL_INTERVENTION_REQUIRED);
            paymentRepositoryPort.update(payment);
            return;
        }

        payment.setState(TransactionState.DEBIT_LEG_REVERSAL_COMPLETED);
        paymentRepositoryPort.update(payment);
        log.info("  [COMPENSATION] Debit Leg reversed successfully. Continuing compensation...");
        compensateLimitEarmark(payment, command);
    }

    private void compensateLimitEarmark(Payment payment, PaymentCommand command) {
        log.warn("  [COMPENSATION] Reversing Limit Earmark for TXN_ID: {}...", payment.getTransactionId());
        var reversalRequest = LimitManagementReversalRequest.builder()
                .transactionId(payment.getTransactionId())
                .limitManagementId(payment.getLimitManagementResponse().getLimitId())
                .build();
        var reversalResponse = limitPort.reverseLimitEarmark(payment.getLimitManagementResponse().getLimitId(), reversalRequest);
        payment.recordLimitReversalResult(reversalResponse);

        if (reversalResponse.getStatus() != LimitEarmarkStatus.REVERSAL_SUCCESSFUL) {
            payment.setState(TransactionState.MANUAL_INTERVENTION_REQUIRED);
        } else {
            payment.setState(TransactionState.TRANSACTION_FAILED);
            log.info("  [COMPENSATION] Limit Earmark reversed successfully. Transaction is FAILED.");
        }
        paymentRepositoryPort.update(payment);
    }

    private static class StepFailedException extends RuntimeException {
        public StepFailedException(String message) {
            super(message);
        }
    }
}
