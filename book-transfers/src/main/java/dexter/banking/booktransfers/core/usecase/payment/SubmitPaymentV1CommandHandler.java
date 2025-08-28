package dexter.banking.booktransfers.core.usecase.payment;
import dexter.banking.booktransfers.core.domain.event.ManualInterventionRequiredEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentSuccessfulEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentFailedEvent;
import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.*;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SubmitPaymentV1CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    private final CreditCardPort creditCardPort;
    private final DepositPort depositPort;
    private final LimitPort limitPort;
    private final PaymentRepositoryPort paymentRepositoryPort;
    private final EventDispatcherPort eventDispatcher;

    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V1;
    }

    @Override
    @Transactional
    public PaymentResult handle(PaymentCommand command) {
        log.info("▶️ [V1] Starting procedural transaction for Command: {}", command.getTransactionReference());
        Payment payment = Payment.startNew(command, UUID::randomUUID);
        paymentRepositoryPort.save(payment);

        try {
            executeLimitEarmark(payment, command);
            executeDebitLeg(payment, command);
            executeCreditLeg(payment, command);

            payment.setState(TransactionState.TRANSACTION_SUCCESSFUL);
            payment.registerEvent(new PaymentSuccessfulEvent(payment.getId(), buildMetadata(command, payment)));
            paymentRepositoryPort.update(payment);
        } catch (StepFailedException e) {
            log.error("❌ [V1] Procedural transaction FAILED at state '{}' for TXN_ID: {}. Initiating SAGA compensation...",
                    payment.getState(), payment.getId(), e);
            Payment lastSavedPayment = paymentRepositoryPort.findById(payment.getId())
                    .orElseThrow(() -> new IllegalStateException("Payment disappeared during failed transaction: " + payment.getId()));
            compensate(lastSavedPayment, command);
        }

        Payment finalPaymentState = paymentRepositoryPort.findById(payment.getId()).orElseThrow();
        eventDispatcher.dispatch(finalPaymentState.pullDomainEvents());
        log.info("🏁 [V1] Procedural transaction finished for TXN_ID: {}. Final state: {}",
                finalPaymentState.getId(), finalPaymentState.getState());
        return PaymentResult.from(finalPaymentState);
    }

    private void executeLimitEarmark(Payment payment, PaymentCommand command) throws StepFailedException {
        var limitRequest = LimitManagementRequest.builder().transactionId(payment.getId()).limitType(command.getLimitType()).build();
        LimitEarmarkResult limitResult = limitPort.earmarkLimit(limitRequest);
        payment.recordLimitEarmarkOutcome(limitResult);

        if (limitResult.status() == LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL) {
            payment.setState(TransactionState.LIMIT_EARMARK_COMPLETED);
        } else {
            payment.setState(TransactionState.TRANSACTION_FAILED);
            payment.registerEvent(new PaymentFailedEvent(payment.getId(), "Limit Earmark Failed", buildMetadata(command, payment)));
            paymentRepositoryPort.update(payment);
            throw new StepFailedException("Limit Earmark Failed");
        }
        paymentRepositoryPort.update(payment);
    }

    private void executeDebitLeg(Payment payment, PaymentCommand command) throws StepFailedException {
        var depositRequest = DepositBankingRequest.builder().transactionId(payment.getId()).accountNumber(command.getAccountNumber()).build();
        DebitLegResult debitResult = depositPort.submitDeposit(depositRequest);
        payment.recordDebitLegOutcome(debitResult);

        if (debitResult.status() == DebitLegResult.DebitLegStatus.SUCCESSFUL) {
            payment.setState(TransactionState.DEBIT_LEG_COMPLETED);
        } else {
            payment.setState(TransactionState.DEBIT_LEG_FAILED);
            paymentRepositoryPort.update(payment);
            throw new StepFailedException("Deposit (Debit) Failed");
        }
        paymentRepositoryPort.update(payment);
    }

    private void executeCreditLeg(Payment payment, PaymentCommand command) throws StepFailedException {
        var creditRequest = CreditCardBankingRequest.builder().transactionId(payment.getId()).cardNumber(command.getCardNumber()).build();
        CreditLegResult creditResult = creditCardPort.submitCreditCardPayment(creditRequest);
        payment.recordCreditResult(creditResult);

        if (creditResult.status() != CreditLegResult.CreditLegStatus.SUCCESSFUL) {
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
                if (payment.pullDomainEvents().isEmpty()) { // Avoid duplicate failure events
                    payment.registerEvent(new PaymentFailedEvent(payment.getId(), "Failed before any action", buildMetadata(command, payment)));
                }
                paymentRepositoryPort.update(payment);
        }
    }

    private void compensateDebitLeg(Payment payment, PaymentCommand command) {
        log.warn("  [COMPENSATION] Reversing Debit Leg for TXN_ID: {}...", payment.getId());
        var reversalRequest = DepositBankingReversalRequest.builder()
                .transactionId(payment.getId())
                .reservationId(payment.getDebitLegResult().depositId())
                .build();
        var reversalResult = depositPort.submitDepositReversal(payment.getDebitLegResult().depositId(), reversalRequest);
        payment.recordDebitLegOutcome(reversalResult);

        if (reversalResult.status() != DebitLegResult.DebitLegStatus.REVERSAL_SUCCESSFUL) {
            payment.setState(TransactionState.MANUAL_INTERVENTION_REQUIRED);
            payment.registerEvent(new ManualInterventionRequiredEvent(payment.getId(), "Debit Leg Reversal Failed", buildMetadata(command, payment)));
            paymentRepositoryPort.update(payment);
            return;
        }

        payment.setState(TransactionState.DEBIT_LEG_REVERSAL_COMPLETED);
        paymentRepositoryPort.update(payment);
        log.info("  [COMPENSATION] Debit Leg reversed successfully. Continuing compensation...");
        compensateLimitEarmark(payment, command);
    }

    private void compensateLimitEarmark(Payment payment, PaymentCommand command) {
        log.warn("  [COMPENSATION] Reversing Limit Earmark for TXN_ID: {}...", payment.getId());
        var reversalRequest = LimitManagementReversalRequest.builder()
                .transactionId(payment.getId())
                .limitManagementId(payment.getLimitEarmarkResult().limitId())
                .build();
        var reversalResult = limitPort.reverseLimitEarmark(payment.getLimitEarmarkResult().limitId(), reversalRequest);
        payment.recordLimitEarmarkOutcome(reversalResult);

        if (reversalResult.status() != LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_SUCCESSFUL) {
            payment.setState(TransactionState.MANUAL_INTERVENTION_REQUIRED);
            payment.registerEvent(new ManualInterventionRequiredEvent(payment.getId(), "Limit Earmark Reversal Failed", buildMetadata(command, payment)));
        } else {
            payment.setState(TransactionState.TRANSACTION_FAILED);
            payment.registerEvent(new PaymentFailedEvent(payment.getId(), "Transaction failed and was fully compensated.", buildMetadata(command, payment)));
            log.info("  [COMPENSATION] Limit Earmark reversed successfully. Transaction is FAILED.");
        }
        paymentRepositoryPort.update(payment);
    }

    private Map<String, Object> buildMetadata(PaymentCommand command, Payment payment) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("webhookUrl", command.getWebhookUrl());
        metadata.put("state", payment.getState());
        metadata.put("transactionReference", payment.getTransactionReference());
        return metadata;
    }

    private static class StepFailedException extends RuntimeException {
        public StepFailedException(String message) {
            super(message);
        }
    }
}
