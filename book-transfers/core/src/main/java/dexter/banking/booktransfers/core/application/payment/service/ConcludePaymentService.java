package dexter.banking.booktransfers.core.application.payment.service;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentFailedUseCase;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentParams;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentRemediationUseCase;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentSuccessUseCase;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.ConfigurationPort;
import dexter.banking.booktransfers.core.port.out.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class ConcludePaymentService implements ConcludePaymentSuccessUseCase, ConcludePaymentRemediationUseCase, ConcludePaymentFailedUseCase {
    private final PaymentRepositoryPort paymentRepository;
    private final EventDispatcherPort eventDispatcher;
    private final BusinessPolicyFactory policyFactory;
    private final ConfigurationPort configurationPort;

    @Override
    public void handleFailure(ConcludePaymentParams params) {
       concludePayment(params.transactionId(), payment -> {
          payment.recordPaymentFailed(params.reason(), buildMetadata(payment, params));
       });
    }

    @Override
    public void handleRemediation(ConcludePaymentParams params) {
        concludePayment(params.transactionId(), payment -> {
            payment.recordPaymentRemediationNeeded(params.reason(), buildMetadata(payment, params));
        });
    }

    @Override
    public void handleSuccess(ConcludePaymentParams params) {
        concludePayment(params.transactionId(), payment -> {
            payment.recordPaymentSettled(buildMetadata(payment, params));
        });
    }

    private void concludePayment(UUID transactionId, Consumer<Payment> recordPaymentAction) {
        Payment.PaymentMemento memento = paymentRepository.findMementoById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + transactionId));
        JourneySpecification spec = configurationPort.findForJourney(memento.journeyName())
                .orElseThrow(() -> new IllegalStateException("No journey configured for identifier: " + memento.journeyName()));
        BusinessPolicy policy = policyFactory.create(spec);

        var payment = Payment.rehydrate(memento, policy);

        recordPaymentAction.accept(payment);
        paymentRepository.update(payment);
        eventDispatcher.dispatch(payment.pullDomainEvents());
    }

    private Map<String, Object> buildMetadata(Payment payment, ConcludePaymentParams params) {
        Map<String, Object> metadata = new HashMap<>(params.metadata());
        metadata.put("transactionReference", payment.getTransactionReference());
        return metadata;
    }
}
