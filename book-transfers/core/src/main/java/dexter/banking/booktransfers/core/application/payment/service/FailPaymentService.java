package dexter.banking.booktransfers.core.application.payment.service;

import dexter.banking.booktransfers.core.application.payment.command.FailPaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.port.in.payment.FailPaymentUseCase;
import dexter.banking.statemachine.StateMachine;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FailPaymentService implements FailPaymentUseCase {

    @Qualifier("v3TransactionFsmFactory")
    private final StateMachineFactory<ProcessStateV3, ProcessEventV3, HybridTransactionContext> stateMachineFactory;

    @Override
    @Transactional
    public void fail(FailPaymentCommand command) {
        // Rehydrate the state machine from persistence
        StateMachine<ProcessStateV3, ProcessEventV3, HybridTransactionContext> stateMachine = stateMachineFactory.acquireStateMachine(command.paymentId().toString())
                .orElseThrow(() -> new IllegalStateException("Could not find state machine for payment ID: " + command.paymentId()));

        // Fire a dedicated failure event to trigger the FSM's compensation path
        stateMachine.fire(ProcessEventV3.COMPLIANCE_REJECTED);
    }
}
