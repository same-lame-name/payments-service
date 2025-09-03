package dexter.banking.booktransfers.core.application.payment.service;

import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.port.in.payment.ResumePaymentParams;
import dexter.banking.booktransfers.core.port.in.payment.ResumePaymentUseCase;
import dexter.banking.statemachine.StateMachine;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ResumePaymentService implements ResumePaymentUseCase {
    @Qualifier("v3TransactionFsmFactory")
    private final StateMachineFactory<ProcessStateV3, ProcessEventV3, HybridTransactionContext> stateMachineFactory;
    @Override
    @Transactional
    public void resume(ResumePaymentParams params) {
        StateMachine<ProcessStateV3, ProcessEventV3, HybridTransactionContext> stateMachine = stateMachineFactory.acquireStateMachine(params.paymentId().toString())
                .orElseThrow(() -> new IllegalStateException("Could not find state machine for payment ID: " + params.paymentId()));
        stateMachine.fire(ProcessEventV3.RESUME);
    }
}
