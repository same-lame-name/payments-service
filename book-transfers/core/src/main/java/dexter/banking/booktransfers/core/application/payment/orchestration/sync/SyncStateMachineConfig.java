package dexter.banking.booktransfers.core.application.payment.orchestration.sync;


import dexter.banking.booktransfers.core.application.payment.orchestration.sync.action.SyncCreditLegAction;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.action.SyncDebitLegAction;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.action.SyncLimitEarmarkAction;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.action.SyncTransactionCompleteAction;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.component.TransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.model.ProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.model.ProcessState;
import dexter.banking.statemachine.StateMachineBuilder;
import dexter.banking.statemachine.StateMachineConfig;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

@Configuration
@RequiredArgsConstructor
public class SyncStateMachineConfig {

    private final SyncLimitEarmarkAction limitEarmarkAction;
    private final SyncDebitLegAction debitLegAction;
    private final SyncCreditLegAction creditLegAction;
    private final SyncTransactionCompleteAction transactionCompleteAction;

    @Bean("syncPaymentStateMachineConfig")
    public StateMachineConfig<ProcessState, ProcessEvent, TransactionContext> syncPaymentStateMachineConfig() {
        return StateMachineBuilder.<ProcessState, ProcessEvent, TransactionContext>newBuilder()
                .states(EnumSet.allOf(ProcessState.class))
                .initial(ProcessState.NEW)
                .end(ProcessState.PROCESS_FAILED)
                .end(ProcessState.PROCESS_COMPLETED)
                .end(ProcessState.REMEDIATION_REQUIRED)
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

    @Bean("syncTransactionFsmFactory")
    public StateMachineFactory<ProcessState, ProcessEvent, TransactionContext> transactionFsmFactory(
            @Qualifier("syncPaymentStateMachineConfig") StateMachineConfig<ProcessState, ProcessEvent, TransactionContext> config) {
        return new StateMachineFactory<>(config);
    }
}
