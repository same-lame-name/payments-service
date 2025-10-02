package dexter.banking.booktransfers.core.domain.shared.blueprint.spec;

import dexter.banking.booktransfers.core.domain.shared.blueprint.ExtractBean;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.LimitPort;


public interface StandardPaymentBlueprint extends BaseJourneyBlueprint {
    AdapterRoutingBlueprint getAdapterRouting();

    interface AdapterRoutingBlueprint extends JourneyBlueprint {
        @ExtractBean
        DepositPort getDepositPort();

        @ExtractBean
        CreditCardPort getCreditCardPort();

        @ExtractBean
        LimitPort getLimitPort();
    }
}
