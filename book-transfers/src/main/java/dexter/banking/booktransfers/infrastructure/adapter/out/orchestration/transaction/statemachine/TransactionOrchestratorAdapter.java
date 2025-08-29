package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.port.TransactionOrchestratorPort;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.component.TransactionStateMachinePersister;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("statemachine")
@Slf4j
public class TransactionOrchestratorAdapter implements TransactionOrchestratorPort {

    private final StateMachineFactory<ProcessState, ProcessEvent, TransactionContext> stateMachineFactory;
    private final TransactionStateMachinePersister persister;

    public TransactionOrchestratorAdapter(
            @Qualifier("transactionFsmFactory") StateMachineFactory<ProcessState, ProcessEvent, TransactionContext> stateMachineFactory,
            TransactionStateMachinePersister persister) {
        this.stateMachineFactory = stateMachineFactory;
        this.persister = persister;
    }

    @Override
    public PaymentResult processTransaction(PaymentCommand command, Payment payment) {
        var context = new TransactionContext(payment, command);
        persister.saveContext(context); // Initial persistence of the orchestration context

        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(ProcessEvent.SUBMIT);
        return PaymentResult.from(context.getPayment());
    }
}
