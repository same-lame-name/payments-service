package dexter.banking.booktransfers.core.domain.shared.blueprint;

import dexter.banking.booktransfers.core.domain.shared.validation.ValidationGroup;
import java.util.List;

public interface JourneyBlueprint {
    String journeyName();
    List<ValidationGroup> validationGroups();
    List<String> dataCollectors();
    List<String> businessRules();
}
