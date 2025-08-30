package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.AsyncOrchestrationEventPort;
import dexter.banking.booktransfers.core.port.TransactionOrchestratorPort;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.component.TransactionStateMachinePersister;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.StateMachine;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

@Component("statemachine")
@Slf4j
public class TransactionOrchestratorAdapter implements TransactionOrchestratorPort, AsyncOrchestrationEventPort {

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

    @Override
    public void processCreditLegResult(UUID transactionId, CreditLegResult result) {
        handleAsyncEvent(transactionId, sm -> {
            var payment = sm.getContext().getPayment();
            payment.recordCredit(result, Collections.emptyMap());
            ProcessEvent event = result.status() == CreditLegResult.CreditLegStatus.SUCCESSFUL ?
                    ProcessEvent.CREDIT_LEG_SUCCEEDED : ProcessEvent.CREDIT_LEG_FAILED;
            sm.fire(event);
        });
    }

    @Override
    public void processDebitLegResult(UUID transactionId, DebitLegResult result) {
        handleAsyncEvent(transactionId, sm -> {
            var payment = sm.getContext().getPayment();
            payment.recordDebit(result, Collections.emptyMap());

            switch (result.status()) {
                case SUCCESSFUL -> sm.fire(ProcessEvent.DEBIT_LEG_SUCCEEDED);
                case FAILED -> sm.fire(ProcessEvent.DEBIT_LEG_FAILED);
                case REVERSAL_SUCCESSFUL -> sm.fire(ProcessEvent.DEBIT_LEG_REVERSAL_SUCCEEDED);
                case REVERSAL_FAILED -> sm.fire(ProcessEvent.DEBIT_LEG_REVERSAL_FAILED);
                default -> throw new IllegalStateException("Unexpected result from debit leg: " + result.status());
            }
        });
    }

    @Override
    public void processLimitEarmarkResult(UUID transactionId, LimitEarmarkResult result) {
        handleAsyncEvent(transactionId, sm -> {
            var payment = sm.getContext().getPayment();
            payment.recordLimitEarmark(result, Collections.emptyMap());

            switch (result.status()) {
                case SUCCESSFUL -> sm.fire(ProcessEvent.LIMIT_EARMARK_SUCCEEDED);
                case FAILED -> sm.fire(ProcessEvent.LIMIT_EARMARK_FAILED);
                case REVERSAL_SUCCESSFUL -> sm.fire(ProcessEvent.LIMIT_EARMARK_REVERSAL_SUCCEEDED);
                case REVERSAL_FAILED -> sm.fire(ProcessEvent.LIMIT_EARMARK_REVERSAL_FAILED);
                default -> throw new IllegalStateException("Unexpected result from debit leg: " + result.status());
            }
        });
    }

    private void handleAsyncEvent(UUID transactionId, Consumer<StateMachine<ProcessState, ProcessEvent, TransactionContext>> handler) {
        stateMachineFactory.acquireStateMachine(transactionId.toString()).ifPresentOrElse(
                handler,
                () -> log.error("Could not acquire state machine for transaction id: {}", transactionId)
        );
    }
}
