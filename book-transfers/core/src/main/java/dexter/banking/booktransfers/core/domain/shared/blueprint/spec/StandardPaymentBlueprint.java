package dexter.banking.booktransfers.core.domain.shared.blueprint.spec;

import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.domain.shared.blueprint.BeanReference;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.validation.ValidationGroup;
import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.statemachine.StateMachineFactory;

import java.util.List;

public interface StandardPaymentBlueprint extends JourneyBlueprint {
    List<ValidationGroup> getValidationGroups();
    List<String> getDataCollectors();
    List<String> getBusinessRules();

    AdapterRoutingBlueprint getAdapterRouting();
    OrchestrationBlueprint getOrchestration();

    interface AdapterRoutingBlueprint extends JourneyBlueprint {
        @BeanReference
        DepositPort getDepositPort();

        @BeanReference
        CreditCardPort getCreditCardPort();

        @BeanReference
        LimitPort getLimitPort();
    }

    interface OrchestrationBlueprint extends JourneyBlueprint {
        @BeanReference
        StateMachineFactory<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> getEngine();
    }
}
