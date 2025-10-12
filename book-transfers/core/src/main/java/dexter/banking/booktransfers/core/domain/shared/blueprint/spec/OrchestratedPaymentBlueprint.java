package dexter.banking.booktransfers.core.domain.shared.blueprint.spec;

import dexter.banking.booktransfers.core.application.payment.orchestration.async.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.AsyncProcessState;
import dexter.banking.booktransfers.core.domain.shared.blueprint.ExtractBean;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.port.out.TransactionLegPort;
import dexter.banking.statemachine.StateMachineFactory;


public interface OrchestratedPaymentBlueprint extends BaseJourneyBlueprint {
    OrchestrationBlueprint getOrchestration();
    AdapterRoutingBlueprint getAdapterRouting();

    interface AdapterRoutingBlueprint extends JourneyBlueprint {
        @ExtractBean
        TransactionLegPort getTransactionLegPort();
    }

    interface OrchestrationBlueprint extends JourneyBlueprint {
        @ExtractBean
        StateMachineFactory<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> getEngine();
    }
}
