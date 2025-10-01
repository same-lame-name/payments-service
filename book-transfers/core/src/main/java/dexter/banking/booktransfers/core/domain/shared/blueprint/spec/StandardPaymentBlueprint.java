package dexter.banking.booktransfers.core.domain.shared.blueprint.spec;

import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.domain.shared.blueprint.BeanReference;
import dexter.banking.booktransfers.core.domain.shared.blueprint.ExtractBean;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.statemachine.StateMachineFactory;


public interface StandardPaymentBlueprint extends BaseJourneyBlueprint {

    AdapterRoutingBlueprint getAdapterRouting();
    OrchestrationBlueprint getOrchestration();

    interface AdapterRoutingBlueprint extends JourneyBlueprint {
        @ExtractBean
//        @BeanReference
        DepositPort getDepositPort();

        @ExtractBean
//        @BeanReference
        CreditCardPort getCreditCardPort();

        @ExtractBean
//        @BeanReference
        LimitPort getLimitPort();
    }

    interface OrchestrationBlueprint extends JourneyBlueprint {
        @ExtractBean
//        @BeanReference
        StateMachineFactory<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> getEngine();
    }
}
