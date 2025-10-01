package dexter.banking.booktransfers.infrastructure.adapter.out.config;

import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
import dexter.banking.booktransfers.core.domain.shared.validation.ValidationGroup;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class JourneyProperties {
    // === COMMON & MANDATORY ===
    private JourneyType journeyType;
    private List<ValidationGroup> validationGroups;
    private List<String> dataCollectors;
    private List<String> businessRules;

    // === COMMON & OPTIONAL (with defaults) ===
    private boolean idempotencyEnabled = true;

    // === SPECIFIC & MANDATORY
    private AdapterRoutingProperties adapterRouting;
    private OrchestrationProperties orchestration;
}
