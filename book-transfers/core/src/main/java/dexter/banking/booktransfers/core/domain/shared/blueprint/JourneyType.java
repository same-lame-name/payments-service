package dexter.banking.booktransfers.core.domain.shared.blueprint;

import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.OrchestratedPaymentBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.StandardPaymentBlueprint;
import lombok.Getter;

@Getter
public enum JourneyType {
    STANDARD_PAYMENT(StandardPaymentBlueprint.class),
    ORCHESTRATED_PAYMENT(OrchestratedPaymentBlueprint.class);

    private final Class<? extends JourneyBlueprint> blueprintClass;

    JourneyType(Class<? extends JourneyBlueprint> blueprintClass) {
        this.blueprintClass = blueprintClass;
    }
}
