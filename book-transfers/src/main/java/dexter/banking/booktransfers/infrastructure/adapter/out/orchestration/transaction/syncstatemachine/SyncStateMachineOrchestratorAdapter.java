package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.port.TransactionOrchestratorPort;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.component.TransactionStateMachinePersister;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.StateMachineFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The primary adapter for the SYNCHRONOUS State Machine.
 * This adapter's processTransaction method IS the Unit of Work boundary.
 */
@Component("syncStatemachine")
public class SyncStateMachineOrchestratorAdapter implements TransactionOrchestratorPort {

    private final StateMachineFactory<ProcessState, ProcessEvent, TransactionContext> stateMachineFactory;
    private final PaymentRepositoryPort paymentRepository;

    public SyncStateMachineOrchestratorAdapter(
            @Qualifier("syncTransactionFsmFactory") StateMachineFactory<ProcessState, ProcessEvent, TransactionContext> stateMachineFactory,
            PaymentRepositoryPort paymentRepository) {
        this.stateMachineFactory = stateMachineFactory;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public PaymentResult processTransaction(PaymentCommand command, Payment payment) {
        // For the sync machine, the live aggregate is passed in the context and mutated in memory.
        var context = new TransactionContext(payment, command);

        // No need for separate persister; the transaction boundary is here.
        // We save the initial state.
        paymentRepository.save(payment);

        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(ProcessEvent.SUBMIT); // This blocks until the FSM reaches a terminal state

        return PaymentResult.from(context.getPayment());
    }
}
