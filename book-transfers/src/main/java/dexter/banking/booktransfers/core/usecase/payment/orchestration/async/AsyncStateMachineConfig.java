package dexter.banking.booktransfers.core.usecase.payment.orchestration.async;

import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.action.TransactionCompleteAction;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.component.TransactionStateMachinePersister;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.statemachine.StateMachineBuilder;
import dexter.banking.statemachine.StateMachineConfig;
import dexter.banking.statemachine.StateMachineFactory;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

@Configuration
@RequiredArgsConstructor
public class AsyncStateMachineConfig {

    private final TransactionStateMachinePersister persister;

    // Correctly qualify the specific SagaAction beans.
    // The generic types must match what the builder expects.
    private final SagaAction<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> limitEarmarkAction;

    private final SagaAction<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> debitLegAction;

    private final SagaAction<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> creditLegAction;

    private final TransactionCompleteAction transactionCompleteAction;


    @Bean("asyncPaymentStateMachineConfig")
    public StateMachineConfig<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> paymentStateMachineConfig() {
        return StateMachineBuilder.<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext>newBuilder()
                .states(EnumSet.allOf(AsyncProcessState.class))
                .initial(AsyncProcessState.NEW)
                .end(AsyncProcessState.PROCESS_FAILED)
                .end(AsyncProcessState.PROCESS_COMPLETED)
                .end(AsyncProcessState.REMEDIATION_REQUIRED)
                .withPersister(persister)
            // --- Happy Path ---
            .from(AsyncProcessState.NEW).on(AsyncProcessEvent.SUBMIT)
                .to(AsyncProcessState.EARMARKING_LIMIT)
                .withAction(limitEarmarkAction::apply)
                .add()
            .from(AsyncProcessState.EARMARKING_LIMIT).on(AsyncProcessEvent.LIMIT_EARMARK_SUCCEEDED)
                .to(AsyncProcessState.DEBITING_FUNDS)
                .withAction(debitLegAction::apply)
                .add()
            .from(AsyncProcessState.DEBITING_FUNDS).on(AsyncProcessEvent.DEBIT_LEG_SUCCEEDED)
                .to(AsyncProcessState.CREDITING_FUNDS)
                .withAction(creditLegAction::apply)
                .add()
            .from(AsyncProcessState.CREDITING_FUNDS).on(AsyncProcessEvent.CREDIT_LEG_SUCCEEDED)
                .to(AsyncProcessState.PROCESS_COMPLETED)
                .withAction(transactionCompleteAction)
                .add()

            // --- Compensation Path ---
            .from(AsyncProcessState.CREDITING_FUNDS).on(AsyncProcessEvent.CREDIT_LEG_FAILED)
                .to(AsyncProcessState.REVERSING_DEBIT)
                .withAction(debitLegAction::compensate)
                .add()
            .from(AsyncProcessState.DEBITING_FUNDS).on(AsyncProcessEvent.DEBIT_LEG_FAILED)
                .to(AsyncProcessState.REVERSING_LIMIT_EARMARK)
                .withAction(limitEarmarkAction::compensate)
                .add()
            .from(AsyncProcessState.EARMARKING_LIMIT).on(AsyncProcessEvent.LIMIT_EARMARK_FAILED)
                .to(AsyncProcessState.PROCESS_FAILED)
                .withAction(transactionCompleteAction)
                .add()
            .from(AsyncProcessState.REVERSING_DEBIT).on(AsyncProcessEvent.DEBIT_LEG_REVERSAL_SUCCEEDED)
                .to(AsyncProcessState.REVERSING_LIMIT_EARMARK)
                .withAction(limitEarmarkAction::compensate)
                .add()
            .from(AsyncProcessState.REVERSING_LIMIT_EARMARK).on(AsyncProcessEvent.LIMIT_EARMARK_REVERSAL_SUCCEEDED)
                .to(AsyncProcessState.PROCESS_FAILED)
                .withAction(transactionCompleteAction)
                .add()

            // --- Remediation Path ---
            .from(AsyncProcessState.REVERSING_DEBIT).on(AsyncProcessEvent.DEBIT_LEG_REVERSAL_FAILED)
                .to(AsyncProcessState.REMEDIATION_REQUIRED)
                .withAction(transactionCompleteAction)
                .add()
            .from(AsyncProcessState.REVERSING_LIMIT_EARMARK).on(AsyncProcessEvent.LIMIT_EARMARK_REVERSAL_FAILED)
                .to(AsyncProcessState.REMEDIATION_REQUIRED)
                .withAction(transactionCompleteAction)
                .add()
            .build();
    }

    @Bean("asyncTransactionFsmFactory")
    public StateMachineFactory<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> transactionFsmFactory(
            @Qualifier("asyncPaymentStateMachineConfig") StateMachineConfig<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> config) {
        return new StateMachineFactory<>(config);
    }
}
