package dexter.banking.booktransfers.core.usecase.payment;

import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.PaymentCommand;
import dexter.banking.booktransfers.core.domain.model.PaymentResponse;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.port.*;
import dexter.banking.booktransfers.core.usecase.payment.mapper.SubmitPaymentV1RequestMapper;
import dexter.banking.booktransfers.core.usecase.payment.mapper.SubmitPaymentV1StatusMapper;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A dedicated application service for the V1 procedural payment flow.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmitPaymentV1CommandHandler implements CommandHandler<PaymentCommand, PaymentResponse> {

    private final CreditCardPort creditCardPort;
    private final DepositPort depositPort;
    private final LimitPort limitPort;
    private final WebhookPort webhookPort;
    private final TransactionRepository transactionRepository;
    private final SubmitPaymentV1StatusMapper statusMapper;
    private final SubmitPaymentV1RequestMapper requestMapper;

    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V1;
    }

    @Override
    public PaymentResponse handle(PaymentCommand command) {
        // CORRECTED: The transactionId is a server-generated, internal identifier.
        // It MUST NOT be coupled to the client-provided idempotencyKey.
        UUID transactionId = UUID.randomUUID();
        log.info("▶️ [V1] Starting procedural transaction for TXN_ID: {}", transactionId);
        PaymentResponse response = PaymentResponse.builder()
                .transactionId(transactionId)
                .transactionReference(command.getTransactionReference())
                .state(TransactionState.NEW)
                .build();
        response = transactionRepository.save(response);

        try {
            executeLimitEarmark(command, response);
            executeDebitLeg(command, response);
            executeCreditLeg(command, response);

            response.setState(TransactionState.TRANSACTION_SUCCESSFUL);
            transactionRepository.update(response);
            log.info("🏁 [V1] Procedural transaction SUCCEEDED for TXN_ID: {}", transactionId);
            webhookPort.notifyTransactionComplete(command.getWebhookUrl(), response.getState());
            return response;
        } catch (Exception e) {
            log.error("❌ [V1] Procedural transaction FAILED at state '{}' for TXN_ID: {}. Initiating SAGA compensation...",
                    response.getState(), transactionId, e);
            PaymentResponse lastSavedResponse = transactionRepository.findByTransactionId(transactionId).orElse(response);

            // Manual Saga Compensation
            switch (lastSavedResponse.getState()) {
                case DEBIT_LEG_IN_PROGRESS:
                    compensateFromCreditFailure(lastSavedResponse, command);
                    break;
                case LIMIT_EARMARK_IN_PROGRESS:
                    compensateFromDebitFailure(lastSavedResponse, command);
                    break;
                default:
                    log.warn("  [COMPENSATION] Failure occurred at initial state. Nothing to compensate.");
                    lastSavedResponse.setState(TransactionState.TRANSACTION_FAILED);
                    transactionRepository.update(lastSavedResponse);
                    webhookPort.notifyTransactionComplete(command.getWebhookUrl(), lastSavedResponse.getState());
            }
            return lastSavedResponse;
        }
    }

    private void executeLimitEarmark(PaymentCommand command, PaymentResponse response) {
        LimitManagementRequest limitRequest = requestMapper.toLimitManagementRequest(response.getTransactionId(), command);
        LimitManagementResponse limitResponse = limitPort.earmarkLimit(limitRequest);
        response.setLimitManagementResponse(limitResponse);
        if (limitResponse.getStatus() != LimitEarmarkStatus.SUCCESSFUL) {
            throw new StepFailedException("Limit Earmark Failed");
        }
        response.setState(TransactionState.LIMIT_EARMARK_IN_PROGRESS);
        transactionRepository.update(response);
    }

    private void executeDebitLeg(PaymentCommand command, PaymentResponse response) {
        DepositBankingRequest depositRequest = requestMapper.toDepositBankingRequest(response.getTransactionId(), command);
        DepositBankingResponse depositResponse = depositPort.submitDeposit(depositRequest);
        response.setDepositBankingResponse(depositResponse);
        if (depositResponse.getStatus() != DepositBankingStatus.SUCCESSFUL) {
            throw new StepFailedException("Deposit (Debit) Failed");
        }
        response.setState(TransactionState.DEBIT_LEG_IN_PROGRESS);
        transactionRepository.update(response);
    }

    private void executeCreditLeg(PaymentCommand command, PaymentResponse response) {
        CreditCardBankingRequest creditCardRequest = requestMapper.toCreditCardBankingRequest(response.getTransactionId(), command);
        CreditCardBankingResponse creditCardResponse = creditCardPort.submitCreditCardPayment(creditCardRequest);
        response.setCreditCardBankingResponse(creditCardResponse);
        if (creditCardResponse.getStatus() != CreditCardBankingStatus.SUCCESSFUL) {
            throw new StepFailedException("Credit Card payment Failed");
        }
    }

    private void compensateFromCreditFailure(PaymentResponse response, PaymentCommand command) {
        log.warn("  [COMPENSATION] Reversing Debit Leg for TXN_ID: {}...", response.getTransactionId());
        DepositBankingReversalRequest reversalRequest = statusMapper.toDepositReversalRequest(response.getTransactionId(), response);
        DepositBankingResponse reversalResponse = depositPort.submitDepositReversal(response.getDepositBankingResponse().getDepositId(), reversalRequest);

        if (reversalResponse.getStatus() != DepositBankingStatus.REVERSAL_SUCCESSFUL) {
            handleFatalReversal("Debit Leg", response, command);
            return;
        }
        log.info("  [COMPENSATION] Debit Leg reversed successfully. Continuing compensation...");
        compensateFromDebitFailure(response, command);
    }

    private void compensateFromDebitFailure(PaymentResponse response, PaymentCommand command) {
        log.warn("  [COMPENSATION] Reversing Limit Earmark for TXN_ID: {}...", response.getTransactionId());
        LimitManagementReversalRequest reversalRequest = statusMapper.toLimitEarmarkReversalRequest(response.getTransactionId(), response);
        LimitManagementResponse reversalResponse = limitPort.reverseLimitEarmark(response.getLimitManagementResponse().getLimitId(), reversalRequest);

        if (reversalResponse.getStatus() != LimitEarmarkStatus.REVERSAL_SUCCESSFUL) {
            handleFatalReversal("Limit Earmark", response, command);
        } else {
            log.info("  [COMPENSATION] Limit Earmark reversed successfully. Transaction is FAILED.");
            response.setState(TransactionState.TRANSACTION_FAILED);
            transactionRepository.update(response);
            webhookPort.notifyTransactionComplete(command.getWebhookUrl(), response.getState());
        }
    }

    private void handleFatalReversal(String leg, PaymentResponse response, PaymentCommand command) {
        log.error("  [FATAL] FAILED to reverse {} for TXN_ID: {}. REQUIRES MANUAL INTERVENTION.", leg, response.getTransactionId());
        response.setState(TransactionState.MANUAL_INTERVENTION_REQUIRED);
        transactionRepository.update(response);
        webhookPort.notifyTransactionComplete(command.getWebhookUrl(), response.getState());
    }

    private static class StepFailedException extends RuntimeException {
        public StepFailedException(String message) {
            super(message);
        }
    }
}
