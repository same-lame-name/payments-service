package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine;

import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.component.SyncTransactionCompleteAction;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.component.TransactionStateMachinePersister;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
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
public class TransactionStateMachineConfig {

    private final TransactionStateMachinePersister persister;
    private final SagaAction<ProcessState, ProcessEvent, TransactionContext> limitEarmarkAction;
    private final SagaAction<ProcessState, ProcessEvent, TransactionContext> debitLegAction;
    private final SagaAction<ProcessState, ProcessEvent, TransactionContext> creditLegAction;
    private final SyncTransactionCompleteAction transactionCompleteAction;


    @Bean
    public StateMachineConfig<ProcessState, ProcessEvent, TransactionContext> paymentStateMachineConfig() {
        return StateMachineBuilder.<ProcessState, ProcessEvent, TransactionContext>newBuilder()
                .states(EnumSet.allOf(ProcessState.class))
                .initial(ProcessState.NEW)
                .end(ProcessState.PROCESS_FAILED)
                .end(ProcessState.PROCESS_COMPLETED)
                .end(ProcessState.REMEDIATION_REQUIRED)
                .withPersister(persister)
            // --- Happy Path ---
            .from(ProcessState.NEW).on(ProcessEvent.SUBMIT)
                .to(ProcessState.EARMARKING_LIMIT)
                .withAction(limitEarmarkAction::apply)
                .add()
            .from(ProcessState.EARMARKING_LIMIT).on(ProcessEvent.LIMIT_EARMARK_SUCCEEDED)
                .to(ProcessState.DEBITING_FUNDS)
                .withAction(debitLegAction::apply)
                .add()
            .from(ProcessState.DEBITING_FUNDS).on(ProcessEvent.DEBIT_LEG_SUCCEEDED)
                .to(ProcessState.CREDITING_FUNDS)
                .withAction(creditLegAction::apply)
                .add()
            .from(ProcessState.CREDITING_FUNDS).on(ProcessEvent.CREDIT_LEG_SUCCEEDED)
                .to(ProcessState.PROCESS_COMPLETED)
                .withAction(transactionCompleteAction)
                .add()

            // --- Compensation Path ---
            .from(ProcessState.CREDITING_FUNDS).on(ProcessEvent.CREDIT_LEG_FAILED)
                .to(ProcessState.REVERSING_DEBIT)
                .withAction(debitLegAction::compensate)
                .add()
            .from(ProcessState.DEBITING_FUNDS).on(ProcessEvent.DEBIT_LEG_FAILED)
                .to(ProcessState.REVERSING_LIMIT_EARMARK)
                .withAction(limitEarmarkAction::compensate)
                .add()
            .from(ProcessState.EARMARKING_LIMIT).on(ProcessEvent.LIMIT_EARMARK_FAILED)
                .to(ProcessState.PROCESS_FAILED)
                .withAction(transactionCompleteAction)
                .add()
            .from(ProcessState.REVERSING_DEBIT).on(ProcessEvent.DEBIT_LEG_REVERSAL_SUCCEEDED)
                .to(ProcessState.REVERSING_LIMIT_EARMARK)
                .withAction(limitEarmarkAction::compensate)
                .add()
            .from(ProcessState.REVERSING_LIMIT_EARMARK).on(ProcessEvent.LIMIT_EARMARK_REVERSAL_SUCCEEDED)
                .to(ProcessState.PROCESS_FAILED)
                .withAction(transactionCompleteAction)
                .add()

            // --- Remediation Path ---
            .from(ProcessState.REVERSING_DEBIT).on(ProcessEvent.DEBIT_LEG_REVERSAL_FAILED)
                .to(ProcessState.REMEDIATION_REQUIRED)
                .withAction(transactionCompleteAction)
                .add()
            .from(ProcessState.REVERSING_LIMIT_EARMARK).on(ProcessEvent.LIMIT_EARMARK_REVERSAL_FAILED)
                .to(ProcessState.REMEDIATION_REQUIRED)
                .withAction(transactionCompleteAction)
                .add()
            .build();
    }

    @Bean
    public StateMachineFactory<ProcessState, ProcessEvent, TransactionContext> transactionFsmFactory(
            @Qualifier("paymentStateMachineConfig") StateMachineConfig<ProcessState, ProcessEvent, TransactionContext> config) {
        return new StateMachineFactory<>(config);
    }
}
