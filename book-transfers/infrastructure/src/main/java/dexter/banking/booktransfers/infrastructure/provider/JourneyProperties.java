package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
import dexter.banking.booktransfers.core.domain.shared.validation.ValidationGroup;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
class JourneyProperties {
    // === MANDATORY ===
    private JourneyType journeyType;
    private List<ValidationGroup> validationGroups;
    private List<String> dataCollectors;
    private List<String> businessRules;
    private AdapterRoutingProperties adapterRouting;
    private OrchestrationProperties orchestration;

    // === OPTIONAL (with defaults) ===
    private boolean idempotencyEnabled = true;
    private List<String> policies = Collections.emptyList();

    @Getter
    @Setter
    public static class AdapterRoutingProperties {
        private String depositPort;
        private String creditCardPort;
        private String limitPort;
        private String transactionLegPort;
    }

    @Getter
    @Setter
    public static class OrchestrationProperties {
        private String engine;
    }
}

