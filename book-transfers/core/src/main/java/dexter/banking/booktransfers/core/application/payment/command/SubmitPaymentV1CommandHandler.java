package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.booktransfers.core.application.payment.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.payment.result.CreditLegResult;
import dexter.banking.booktransfers.core.domain.payment.result.DebitLegResult;
import dexter.banking.booktransfers.core.domain.payment.result.LimitEarmarkResult;
import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContextHolder;
import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.*;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Command Handler for the V1 procedural flow.
 * This handler is now a self-contained, transactional SAGA.
 * It no longer depends
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
    private final BusinessPolicyFactory policyFactory;

    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V1;
    }

    @Override
    @Transactional
    public PaymentResult handle(PaymentCommand command) {
        log.info("▶️ [V1] Starting procedural transaction for Command: {}", command.getTransactionReference());

        JourneySpecification spec = CommandProcessingContextHolder.getContext()
                .map(CommandProcessingContext::getJourneySpecification)
                .orElseThrow(() -> new IllegalStateException("JourneySpecification not found in context"));

        BusinessPolicy policy = policyFactory.create(spec);

        String journeyIdentifier = command.getIdentifier();
        String reasonForFailure = "";

        Payment payment = Payment.startNew(command, policy, journeyIdentifier);
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
            reasonForFailure = Optional.of(e).map(Throwable::getMessage).orElse("Unknown error");
            compensate(payment, command);
        } finally {

            switch(payment.getState()) {
                case FUNDS_CREDITED -> payment.recordPaymentSettled(buildMetadata(command, payment));
                case LIMIT_COULD_NOT_BE_RESERVED, LIMIT_REVERSED -> payment.recordPaymentFailed(reasonForFailure, buildMetadata(command, payment));
                default -> payment.recordPaymentRemediationNeeded(reasonForFailure, buildMetadata(command, payment));
            }

            paymentRepository.update(payment);
            eventDispatcher.dispatch(payment.pullDomainEvents());
        }

        log.info("🏁 [V1] Procedural transaction finished for TXN_ID: {}. Final state: {}",
                payment.getId(), payment.getState());
        return PaymentResult.from(payment);
    }

    private void performCreditLeg(PaymentCommand command, Payment payment) {
        CreditLegResult creditResult = creditCardPort.submitCreditCardPayment(command);
        payment.recordCredit(creditResult, buildMetadata(command, payment));
        paymentRepository.update(payment);

        if (creditResult.status() == CreditLegResult.CreditLegStatus.FAILED) {
            throw new StepFailedException("Credit Leg failed");
        }

    }

    private void performDebitLeg(PaymentCommand command, Payment payment) {
        DebitLegResult debitResult = depositPort.submitDeposit(command);
        payment.recordDebit(debitResult, buildMetadata(command, payment));
        paymentRepository.update(payment);

        if (debitResult.status() == DebitLegResult.DebitLegStatus.FAILED) {
            throw new StepFailedException("Debit Leg failed");
        }
    }

    private void performLimitEarmark(PaymentCommand command, Payment payment) {
        LimitEarmarkResult limitResult = limitPort.earmarkLimit(command);
        payment.recordLimitEarmark(limitResult, buildMetadata(command, payment));
        paymentRepository.update(payment);

        if (limitResult.status() == LimitEarmarkResult.LimitEarmarkStatus.FAILED) {
            throw new StepFailedException("Limit Earmark failed");
        }
    }

    private void compensate(Payment payment, PaymentCommand command) {
        // SAGA compensation logic
        switch (payment.getState()) {
            case FUNDS_COULD_NOT_BE_CREDITED:
                compensateDebitLeg(payment, command);
                break;
            case FUNDS_COULD_NOT_BE_DEBITED:
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
            DebitLegResult reversalResult = depositPort.submitDepositReversal(payment);

            payment.recordDebitReversal(reversalResult, buildMetadata(command, payment));
            paymentRepository.update(payment);

            if (reversalResult.status() == DebitLegResult.DebitLegStatus.REVERSAL_SUCCESSFUL) {
                log.info("  [COMPENSATION] Debit leg reversed successfully. Compensation saga in progress.");
                compensateLimitEarmark(payment, command);
            } else {
                log.error("  [COMPENSATION] FATAL: Reversing Debit Leg FAILED. Manual intervention required.");
            }

        } catch (Exception e) {
            log.error("  [COMPENSATION] FATAL: Reversing Debit Leg FAILED. Manual intervention required.", e);
            payment.recordDebitReversal(new DebitLegResult(null, DebitLegResult.DebitLegStatus.REVERSAL_FAILED), buildMetadata(command, payment));
            paymentRepository.update(payment);
        }
    }

    private void compensateLimitEarmark(Payment payment, PaymentCommand command) {
        log.warn("  [COMPENSATION] Reversing Limit Earmark for TXN_ID: {}...", payment.getId());
        try {
            LimitEarmarkResult reversalResult = limitPort.reverseLimitEarmark(payment);
            payment.recordLimitReversal(reversalResult, buildMetadata(command, payment));
        } catch (Exception e) {
            log.error("  [COMPENSATION] FATAL: Reversing Limit Earmark FAILED. Manual intervention required.", e);
            payment.recordLimitReversal(new LimitEarmarkResult(null, LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_FAILED), buildMetadata(command, payment));
        } finally {
            paymentRepository.update(payment);
        }
    }

    private Map<String, Object> buildMetadata(PaymentCommand command, Payment payment) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transactionReference", payment.getTransactionReference());
        return metadata;
    }

    private static class StepFailedException extends RuntimeException {
        public StepFailedException(String message) {
            super(message);
        }
    }
}
