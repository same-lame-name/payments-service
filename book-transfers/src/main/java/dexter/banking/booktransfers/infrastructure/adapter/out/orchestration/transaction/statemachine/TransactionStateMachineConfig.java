package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine;

import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.guard.DebitLegSucceededGuard;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.guard.LimitEarmarkSucceededGuard;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.component.EventDispatchingInterceptor;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.component.TransactionStateMachinePersister;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.component.RegisterDomainEventInterceptor;
import dexter.banking.statemachine.StateMachineFactory;
import dexter.banking.statemachine.contract.Action;
import dexter.banking.statemachine.contract.SagaAction;
import dexter.banking.statemachine.StateMachineBuilder;
import dexter.banking.statemachine.StateMachineConfig;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

@Configuration
@RequiredArgsConstructor
public class TransactionStateMachineConfig {

    private final TransactionStateMachinePersister persister;
    private final EventDispatchingInterceptor eventDispatchingInterceptor;
    private final RegisterDomainEventInterceptor registerDomainEventInterceptor;
    private final SagaAction<TransactionState, TransactionEvent, TransactionContext> limitEarmarkAction;
    private final SagaAction<TransactionState, TransactionEvent, TransactionContext> debitLegAction;
    private final SagaAction<TransactionState, TransactionEvent, TransactionContext> creditLegAction;
    private final Action<TransactionState, TransactionEvent, TransactionContext> transactionCompleteAction;

    private final LimitEarmarkSucceededGuard limitEarmarkSucceededGuard;
    private final DebitLegSucceededGuard debitLegSucceededGuard;

    @Bean
    public StateMachineConfig<TransactionState, TransactionEvent, TransactionContext> paymentStateMachineConfig() {
        return StateMachineBuilder.<TransactionState, TransactionEvent, TransactionContext>newBuilder()
                .states(EnumSet.allOf(TransactionState.class))
                .initial(TransactionState.NEW)
                .end(TransactionState.TRANSACTION_FAILED)
                .end(TransactionState.TRANSACTION_SUCCESSFUL)
                .end(TransactionState.MANUAL_INTERVENTION_REQUIRED)
                .withPersister(persister)
                .addInterceptor(registerDomainEventInterceptor)
                .addInterceptor(eventDispatchingInterceptor) // This now handles event dispatch for all FSM transitions
            .from(TransactionState.NEW).on(TransactionEvent.SUBMIT)
                .to(TransactionState.LIMIT_EARMARK_IN_PROGRESS)
                .withAction(limitEarmarkAction::apply)
                .add()
            .from(TransactionState.LIMIT_EARMARK_IN_PROGRESS).on(TransactionEvent.LIMIT_EARMARK_SUCCEEDED)
                .to(TransactionState.DEBIT_LEG_IN_PROGRESS)
                .withAction(debitLegAction::apply)
                .withGuard(limitEarmarkSucceededGuard)
                .add()
            .from(TransactionState.DEBIT_LEG_IN_PROGRESS).on(TransactionEvent.DEBIT_LEG_SUCCEEDED)
                .to(TransactionState.CREDIT_LEG_IN_PROGRESS)
                .withAction(creditLegAction::apply)
                .withGuard(debitLegSucceededGuard)
                .add()
            .from(TransactionState.CREDIT_LEG_IN_PROGRESS).on(TransactionEvent.CREDIT_LEG_SUCCEEDED)
                .to(TransactionState.TRANSACTION_SUCCESSFUL)
                .withAction(transactionCompleteAction)
                .add()
            .from(TransactionState.CREDIT_LEG_IN_PROGRESS).on(TransactionEvent.CREDIT_LEG_FAILED)
                .to(TransactionState.DEBIT_LEG_REVERSAL_IN_PROGRESS)
                .withAction(debitLegAction::compensate)
                .add()
            .from(TransactionState.DEBIT_LEG_IN_PROGRESS).on(TransactionEvent.DEBIT_LEG_FAILED)
                .to(TransactionState.LIMIT_EARMARK_REVERSAL_IN_PROGRESS)
                .withAction(limitEarmarkAction::compensate)
                .add()
            .from(TransactionState.LIMIT_EARMARK_IN_PROGRESS).on(TransactionEvent.LIMIT_EARMARK_FAILED)
                .to(TransactionState.TRANSACTION_FAILED)
                .withAction(transactionCompleteAction)
                .add()
            .from(TransactionState.DEBIT_LEG_REVERSAL_IN_PROGRESS).on(TransactionEvent.DEBIT_LEG_REVERSAL_SUCCEEDED)
                .to(TransactionState.LIMIT_EARMARK_REVERSAL_IN_PROGRESS)
                .withAction(limitEarmarkAction::compensate)
                .add()
            .from(TransactionState.DEBIT_LEG_REVERSAL_IN_PROGRESS).on(TransactionEvent.DEBIT_LEG_REVERSAL_FAILED)
                .to(TransactionState.MANUAL_INTERVENTION_REQUIRED)
                .withAction(transactionCompleteAction)
                .add()
            .from(TransactionState.LIMIT_EARMARK_REVERSAL_IN_PROGRESS).on(TransactionEvent.LIMIT_EARMARK_REVERSAL_SUCCEEDED)
                .to(TransactionState.TRANSACTION_FAILED)
                .withAction(transactionCompleteAction)
                .add()
            .from(TransactionState.LIMIT_EARMARK_REVERSAL_IN_PROGRESS).on(TransactionEvent.LIMIT_EARMARK_REVERSAL_FAILED)
                .to(TransactionState.MANUAL_INTERVENTION_REQUIRED)
                .withAction(transactionCompleteAction)
                .add()
            .build();
    }

    @Bean
    public StateMachineFactory<TransactionState, TransactionEvent, TransactionContext> transactionFsmFactory(
            @Qualifier("paymentStateMachineConfig") StateMachineConfig<TransactionState, TransactionEvent, TransactionContext> config) {
        return new StateMachineFactory<>(config);
    }
}
