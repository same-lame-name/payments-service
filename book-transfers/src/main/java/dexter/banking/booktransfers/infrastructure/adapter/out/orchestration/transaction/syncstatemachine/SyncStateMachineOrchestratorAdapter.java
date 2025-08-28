package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.port.TransactionOrchestratorPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.component.TransactionStateMachinePersister;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.StateMachineFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The primary adapter for the SYNCHRONOUS State Machine.
 */
@Component("syncStatemachine")
public class SyncStateMachineOrchestratorAdapter implements TransactionOrchestratorPort {

    private final StateMachineFactory<TransactionState, TransactionEvent, TransactionContext> stateMachineFactory;

    private final TransactionStateMachinePersister persister;
    public SyncStateMachineOrchestratorAdapter(
            @Qualifier("syncTransactionFsmFactory") StateMachineFactory<TransactionState, TransactionEvent, TransactionContext> stateMachineFactory,
            TransactionStateMachinePersister persister) {
        this.stateMachineFactory = stateMachineFactory;
        this.persister = persister;
    }

    @Override
    public PaymentResult processTransaction(PaymentCommand command) {
        Payment payment = Payment.startNew(command, UUID::randomUUID);
        var context = new TransactionContext(payment, command);
        persister.saveContext(context);

        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(TransactionEvent.SUBMIT);

        return PaymentResult.from(context.getPayment());
    }
}
