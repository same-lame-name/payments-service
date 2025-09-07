package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.action;

import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.ConfigurationPort;
import dexter.banking.booktransfers.core.port.out.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.statemachine.contract.Action;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ComplianceDecisionAction implements Action<ProcessStateV3, ProcessEventV3, HybridTransactionContext> {

    private final ConfigurationPort configurationPort;
    private final PaymentRepositoryPort paymentRepository;
    private final BusinessPolicyFactory policyFactory;
    private final EventDispatcherPort eventDispatcher;

    @Override
    @Transactional
    public Optional<ProcessEventV3> execute(HybridTransactionContext context, ProcessEventV3 event) {
        Payment payment = rehydratePayment(context);
        JourneySpecification journeySpec = configurationPort.findForJourney(payment.getJourneyName())
                .orElseThrow(() -> new IllegalStateException("Journey specification not found for: " + payment.getJourneyName()));

        BigDecimal complianceThreshold = journeySpec.complianceThreshold()
                .orElseThrow(() -> new IllegalStateException("Compliance threshold is not configured for this journey."));

        BigDecimal amount = context.getTransactionAmount().amount();

        if (amount.compareTo(complianceThreshold) >= 0) {
            payment.flagForComplianceCheck(Collections.emptyMap());
            paymentRepository.update(payment);

            eventDispatcher.dispatch(payment.pullDomainEvents());
            return Optional.of(ProcessEventV3.COMPLIANCE_PENDING);
        } else {
            paymentRepository.update(payment);
            return Optional.of(ProcessEventV3.COMPLIANCE_NOT_REQUIRED);
        }
    }

    private Payment rehydratePayment(HybridTransactionContext context) {
        UUID transactionId = context.getPaymentId();
        Payment.PaymentMemento memento = paymentRepository.findMementoById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + transactionId));
        BusinessPolicy policy = configurationPort
                .findForJourney(memento.journeyName())
                .map(policyFactory::create)
                .orElseThrow(() -> new IllegalStateException("No journey configured for identifier: " + memento.journeyName()));
        return Payment.rehydrate(memento, policy);
    }
}
