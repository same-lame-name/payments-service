package dexter.banking.booktransfers.core.application.payment.orchestration.async.action;

import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.shared.blueprint.BlueprintAccessor;
import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.OrchestratedPaymentBlueprint;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.port.out.TransactionLegPort;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("debitLegAction")
@RequiredArgsConstructor
public class DebitLegAction implements SagaAction<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> {

    private final PaymentRepositoryPort paymentRepository;
    private final BlueprintAccessor blueprintAccessor;

    @Override
    public Optional<AsyncProcessEvent> apply(AsyncTransactionContext context, AsyncProcessEvent event) {
        var transactionLegPort = getTransactionLegPort();
        var request = new DepositPort.SubmitDepositRequest(context.getPaymentId(), context.getAccountNumber());
        transactionLegPort.sendDepositRequest(request);
        return Optional.empty();
    }

    @Override
    public Optional<AsyncProcessEvent> compensate(AsyncTransactionContext context, AsyncProcessEvent event) {
        var transactionLegPort = getTransactionLegPort();
        Payment.PaymentMemento memento = paymentRepository.findMementoById(context.getPaymentId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + context.getPaymentId()));

        var request = new DepositPort.SubmitDepositReversalRequest(
                memento.id(),
                memento.debitLegResult().depositId()
        );
        transactionLegPort.sendDepositReversalRequest(request);
        return Optional.empty();
    }

    private TransactionLegPort getTransactionLegPort() {
        OrchestratedPaymentBlueprint blueprint = blueprintAccessor.get();

        return blueprint.getAdapterRouting().getTransactionLegPort();
    }
}
