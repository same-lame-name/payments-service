package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid;

import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.action.*;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridStateMachinePersister;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
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
public class V3StateMachineConfig {

    private final SyncLimitCheckAction syncLimitCheckAction;
    private final ComplianceDecisionAction complianceDecisionAction;
    private final AsyncDebitLegAction asyncDebitLegAction;
    private final AsyncCreditLegAction asyncCreditLegAction;
    private final TransactionCompleteActionV3 transactionCompleteAction;
    private final HybridStateMachinePersister persister;

    @Bean("v3PaymentStateMachineConfig")
    public StateMachineConfig<ProcessStateV3, ProcessEventV3, HybridTransactionContext> paymentStateMachineConfig() {
        return StateMachineBuilder.<ProcessStateV3, ProcessEventV3, HybridTransactionContext>newBuilder()
                .states(EnumSet.allOf(ProcessStateV3.class))
                .initial(ProcessStateV3.NEW)
                .end(ProcessStateV3.PROCESS_FAILED)
                .end(ProcessStateV3.PROCESS_SETTLED)
                .end(ProcessStateV3.REMEDIATION_REQUIRED)
                .withPersister(persister)

                // 1. Initial synchronous part
                .from(ProcessStateV3.NEW).on(ProcessEventV3.SUBMIT)
                .to(ProcessStateV3.EARMARKING_LIMIT)
                .withAction(syncLimitCheckAction::apply)
                .add()

                // 2. Cascade to compliance check
                .from(ProcessStateV3.EARMARKING_LIMIT).on(ProcessEventV3.LIMIT_APPROVED)
                .to(ProcessStateV3.CHECKING_COMPLIANCE)
                .withAction(complianceDecisionAction)
                .add()

                // 3. Pause for compliance or proceed to debit
                .from(ProcessStateV3.CHECKING_COMPLIANCE).on(ProcessEventV3.COMPLIANCE_NOT_REQUIRED)
                .to(ProcessStateV3.DEBITING_FUNDS)
                .withAction(asyncDebitLegAction::apply)
                .add()
                .from(ProcessStateV3.CHECKING_COMPLIANCE).on(ProcessEventV3.COMPLIANCE_PENDING)
                .to(ProcessStateV3.AWAITING_COMPLIANCE_APPROVAL)
                .add() // PAUSE

                // 4. Resume from compliance
                .from(ProcessStateV3.AWAITING_COMPLIANCE_APPROVAL).on(ProcessEventV3.RESUME)
                .to(ProcessStateV3.DEBITING_FUNDS)
                .withAction(asyncDebitLegAction::apply)
                .add()

                // 5. Async debit/credit legs
                .from(ProcessStateV3.DEBITING_FUNDS).on(ProcessEventV3.DEBIT_LEG_SUCCEEDED)
                .to(ProcessStateV3.CREDITING_FUNDS)
                .withAction(asyncCreditLegAction::apply)
                .add()
                .from(ProcessStateV3.CREDITING_FUNDS).on(ProcessEventV3.CREDIT_LEG_SUCCEEDED)
                .to(ProcessStateV3.PROCESS_SETTLED)
                .withAction(transactionCompleteAction)
                .add()

                // --- Compensation Path ---
                .from(ProcessStateV3.EARMARKING_LIMIT).on(ProcessEventV3.LIMIT_REJECTED)
                .to(ProcessStateV3.PROCESS_FAILED)
                .withAction(transactionCompleteAction)
                .add()
                .from(ProcessStateV3.AWAITING_COMPLIANCE_APPROVAL).on(ProcessEventV3.COMPLIANCE_REJECTED) // New failure path
                .to(ProcessStateV3.REVERSING_LIMIT_EARMARK)
                .withAction(syncLimitCheckAction::compensate)
                .add()
                .from(ProcessStateV3.CREDITING_FUNDS).on(ProcessEventV3.CREDIT_LEG_FAILED)
                .to(ProcessStateV3.REVERSING_DEBIT)
                .withAction(asyncDebitLegAction::compensate)
                .add()
                .from(ProcessStateV3.DEBITING_FUNDS).on(ProcessEventV3.DEBIT_LEG_FAILED)
                .to(ProcessStateV3.REVERSING_LIMIT_EARMARK)
                .withAction(syncLimitCheckAction::compensate)
                .add()
                .from(ProcessStateV3.REVERSING_DEBIT).on(ProcessEventV3.DEBIT_LEG_REVERSAL_SUCCEEDED)
                .to(ProcessStateV3.REVERSING_LIMIT_EARMARK)
                .withAction(syncLimitCheckAction::compensate)
                .add()
                .from(ProcessStateV3.REVERSING_LIMIT_EARMARK).on(ProcessEventV3.LIMIT_EARMARK_REVERSAL_SUCCEEDED)
                .to(ProcessStateV3.PROCESS_FAILED)
                .withAction(transactionCompleteAction)
                .add()

                // --- Remediation Path ---
                .from(ProcessStateV3.REVERSING_DEBIT).on(ProcessEventV3.DEBIT_LEG_REVERSAL_FAILED)
                .to(ProcessStateV3.REMEDIATION_REQUIRED)
                .withAction(transactionCompleteAction)
                .add()
                .from(ProcessStateV3.REVERSING_LIMIT_EARMARK).on(ProcessEventV3.LIMIT_EARMARK_REVERSAL_FAILED)
                .to(ProcessStateV3.REMEDIATION_REQUIRED)
                .withAction(transactionCompleteAction)
                .add()

                .build();
    }

    @Bean("v3TransactionFsmFactory")
    public StateMachineFactory<ProcessStateV3, ProcessEventV3, HybridTransactionContext> transactionFsmFactory(
            @Qualifier("v3PaymentStateMachineConfig") StateMachineConfig<ProcessStateV3, ProcessEventV3, HybridTransactionContext> config) {
        return new StateMachineFactory<>(config);
    }
}
