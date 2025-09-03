package dexter.banking.booktransfers.core.application.payment.command;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.CreditLegResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.DebitLegResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.LimitEarmarkResult;
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

        var creationParams = new Payment.PaymentCreationParams(
                command.getTransactionId(),
                command.getTransactionReference()
        );
        Payment payment = Payment.startNew(creationParams, policy, journeyIdentifier);
        paymentRepository.save(payment);
        try {
            performLimitEarmark(command, payment);
            performDebitLeg(command, payment);
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
        var request = new CreditCardPort.SubmitCreditCardPaymentRequest(command.getTransactionId(), command.getCardNumber());
        CreditLegResult creditResult = creditCardPort.submitCreditCardPayment(request);
        payment.recordCredit(creditResult, buildMetadata(command, payment));
        paymentRepository.update(payment);

        if (creditResult.status() == CreditLegResult.CreditLegStatus.FAILED) {
            throw new StepFailedException("Credit Leg failed");
        }

    }

    private void performDebitLeg(PaymentCommand command, Payment payment) {
        var request = new DepositPort.SubmitDepositRequest(command.getTransactionId(), command.getAccountNumber());
        DebitLegResult debitResult = depositPort.submitDeposit(request);
        payment.recordDebit(debitResult, buildMetadata(command, payment));
        paymentRepository.update(payment);

        if (debitResult.status() == DebitLegResult.DebitLegStatus.FAILED) {
            throw new StepFailedException("Debit Leg failed");
        }
    }

    private void performLimitEarmark(PaymentCommand command, Payment payment) {
        var request = new LimitPort.EarmarkLimitRequest(command.getTransactionId(), command.getLimitType());
        LimitEarmarkResult limitResult = limitPort.earmarkLimit(request);
        payment.recordLimitEarmark(limitResult, buildMetadata(command, payment));
        paymentRepository.update(payment);

        if (limitResult.status() == LimitEarmarkResult.LimitEarmarkStatus.FAILED) {
            throw new StepFailedException("Limit Earmark failed");
        }
    }

    private void compensate(Payment payment, PaymentCommand command) {
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
            var request = new DepositPort.SubmitDepositReversalRequest(payment.getId(), payment.getDebitLegResult().depositId());
            DebitLegResult reversalResult = depositPort.submitDepositReversal(request);

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
            var request = new LimitPort.ReverseLimitEarmarkRequest(payment.getId(), payment.getLimitEarmarkResult().limitId());
            LimitEarmarkResult reversalResult = limitPort.reverseLimitEarmark(request);
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
