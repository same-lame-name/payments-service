package dexter.banking.booktransfers.core.application.payment.service;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.OrchestratedPaymentBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.BlueprintAccessor;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentFailedUseCase;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentParams;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentRemediationUseCase;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentSuccessUseCase;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
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
class ConcludePaymentService implements ConcludePaymentSuccessUseCase, ConcludePaymentRemediationUseCase, ConcludePaymentFailedUseCase {
    private final PaymentRepositoryPort paymentRepository;
    private final EventDispatcherPort eventDispatcher;
    private final BusinessPolicyFactory policyFactory;
    private final BlueprintAccessor blueprintAccessor;

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
        OrchestratedPaymentBlueprint blueprint = blueprintAccessor.get();
        BusinessPolicy policy = policyFactory.create(blueprint.getPolicies());
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
