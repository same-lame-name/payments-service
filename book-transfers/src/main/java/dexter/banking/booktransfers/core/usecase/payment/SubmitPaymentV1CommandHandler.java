package dexter.banking.booktransfers.core.usecase.payment;

import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.domain.model.config.CommandConfiguration;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.ConfigurationPort;
import dexter.banking.booktransfers.core.port.CreditCardPort;
import dexter.banking.booktransfers.core.port.DepositPort;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.LimitPort;
import dexter.banking.booktransfers.core.port.PaymentPolicyFactory;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Command Handler for the V1 procedural flow.
 * This handler is now a self-contained, transactional SAGA. It no longer depends
 * on a state machine and directly orchestrates the calls to external services,
 * updating the aggregate's state, and performing its own compensation logic in case of failure.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SubmitPaymentV1CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    private final CreditCardPort creditCardPort;
    private final DepositPort depositPort;
    private final LimitPort limitPort;
    private final PaymentRepositoryPort paymentRepository;
    private final EventDispatcherPort eventDispatcher;
    private final PaymentPolicyFactory policyFactory;
    private final ConfigurationPort configurationPort;

    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V1;
    }

    @Override
    @Transactional
    public PaymentResult handle(PaymentCommand command) {
        log.info("▶️ [V1] Starting procedural transaction for Command: {}", command.getTransactionReference());

        String journeyName = configurationPort.findForCommand(command.getIdentifier())
                .map(CommandConfiguration::journeyName)
                .orElseThrow(() -> new IllegalStateException("No journey configured for command: " + command.getIdentifier()));

        BusinessPolicy policy = policyFactory.getPolicyForJourney(journeyName);

        Payment payment = Payment.startNew(command, policy, journeyName);
        paymentRepository.save(payment);

        try {
            // Step 1: Limit Earmark
            performLimitEarmark(command, payment);

            // Step 2: Debit Leg
            performDebitLeg(command, payment);

            // Step 3: Credit Leg
            performCreditLeg(command, payment);

        } catch (Exception e) {
            log.error("❌ [V1] Procedural transaction FAILED for TXN_ID: {}. Initiating SAGA compensation...",
                    payment.getId(), e);
            compensate(payment, command);
        }

        eventDispatcher.dispatch(payment.pullDomainEvents());

        log.info("🏁 [V1] Procedural transaction finished for TXN_ID: {}. Final state: {}",
                payment.getId(), payment.getState());
        return PaymentResult.from(payment);
    }

    private void performCreditLeg(PaymentCommand command, Payment payment) {
        CreditLegResult creditResult = creditCardPort.submitCreditCardPayment(
                new CreditCardBankingRequest(payment.getId(), command.getCardNumber())
        );

        if (creditResult.status() == CreditLegResult.CreditLegStatus.SUCCESSFUL) {
            payment.recordCreditSuccess(creditResult, buildMetadata(command, payment));
        } else {
            payment.recordCreditFailure(creditResult, buildMetadata(command, payment));
        }

        paymentRepository.update(payment);
    }

    private void performDebitLeg(PaymentCommand command, Payment payment) {
        DebitLegResult debitResult = depositPort.submitDeposit(
                new DepositBankingRequest(payment.getId(), command.getAccountNumber())
        );

        if (debitResult.status() == DebitLegResult.DebitLegStatus.SUCCESSFUL) {
            payment.recordDebitSuccess(debitResult, buildMetadata(command, payment));
        } else {
            payment.recordDebitFailure(debitResult, buildMetadata(command, payment));
        }

        paymentRepository.update(payment);
    }

    private void performLimitEarmark(PaymentCommand command, Payment payment) {
        LimitEarmarkResult limitResult = limitPort.earmarkLimit(
                new LimitManagementRequest(payment.getId(), command.getLimitType())
        );

        if (limitResult.status() == LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL) {
            payment.recordLimitEarmarkSuccess(limitResult, buildMetadata(command, payment));
        } else {
            payment.recordLimitEarmarkFailure(limitResult, buildMetadata(command, payment));
            throw new StepFailedException("Limit Earmark failed");
        }

        paymentRepository.update(payment);
    }

    private void compensate(Payment payment, PaymentCommand command) {
        // SAGA compensation logic
        switch (payment.getState()) {
            case FUNDS_DEBITED:
                compensateDebitLeg(payment, command);
                break;
            case LIMIT_RESERVED:
                compensateLimitEarmark(payment, command);
                break;
            default:
                log.warn("  [COMPENSATION] Failure occurred at state {}. No compensation needed.", payment.getState());
                break;
        }
    }

    private void compensateDebitLeg(Payment payment, PaymentCommand command) {
        log.warn("  [COMPENSATION] Reversing Debit Leg for TXN_ID: {}...", payment.getId());
        try {
            var reversalRequest = new DepositBankingReversalRequest(payment.getId(), payment.getDebitLegResult().depositId());
            DebitLegResult reversalResult = depositPort.submitDepositReversal(payment.getDebitLegResult().depositId(), reversalRequest);
            if (reversalResult.status() == DebitLegResult.DebitLegStatus.SUCCESSFUL) {
                log.info("  [COMPENSATION] Debit leg reversed successfully. Transaction is FAILED.");
                payment.recordDebitReversalSuccess(reversalResult, buildMetadata(command, payment));
                paymentRepository.update(payment);

                compensateLimitEarmark(payment, command);
            } else {
                log.error("  [COMPENSATION] FATAL: Reversing Debit Leg FAILED. Manual intervention required.");
                payment.recordDebitReversalFailure(reversalResult, buildMetadata(command, payment));
                paymentRepository.update(payment);
            }

        } catch (Exception e) {
            log.error("  [COMPENSATION] FATAL: Reversing Debit Leg FAILED. Manual intervention required.", e);
            payment.recordDebitReversalFailure(payment.getDebitLegResult(), buildMetadata(command, payment));

            paymentRepository.update(payment);
        }
    }

    private void compensateLimitEarmark(Payment payment, PaymentCommand command) {
        log.warn("  [COMPENSATION] Reversing Limit Earmark for TXN_ID: {}...", payment.getId());
        try {
            var reversalRequest = new LimitManagementReversalRequest(payment.getId(), payment.getLimitEarmarkResult().limitId());
            LimitEarmarkResult reversalResult = limitPort.reverseLimitEarmark(payment.getLimitEarmarkResult().limitId(), reversalRequest);
            if (reversalResult.status() == LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL) {
                log.info("  [COMPENSATION] Limit Earmark reversed successfully. Transaction is FAILED.");
                payment.recordLimitReversalSuccess(reversalResult, buildMetadata(command, payment));
            } else {
                log.error("  [COMPENSATION] FATAL: Reversing Limit Earmark FAILED. Manual intervention required.");
                payment.recordLimitReversalFailure(payment.getLimitEarmarkResult(), buildMetadata(command, payment));
            }

        } catch (Exception e) {
            log.error("  [COMPENSATION] FATAL: Reversing Limit Earmark FAILED. Manual intervention required.", e);
            payment.recordLimitReversalFailure(payment.getLimitEarmarkResult(), buildMetadata(command, payment));
        } finally {
            paymentRepository.update(payment);
        }
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
