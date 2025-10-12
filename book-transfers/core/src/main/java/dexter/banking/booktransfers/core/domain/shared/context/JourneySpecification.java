package dexter.banking.booktransfers.core.domain.shared.context;

import dexter.banking.booktransfers.core.domain.featureflag.UserGroup;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@Getter
@RequiredArgsConstructor
public class JourneySpecification {
    private final String journeyName;
    private final JourneyType journeyType;
    private final JourneyBlueprint blueprint;
    private final FeatureFlag featureFlag;

    public record FeatureFlag(boolean enabled, Set<UserGroup> pilotGroups) {
        public FeatureFlag {
            pilotGroups = (pilotGroups == null) ? Set.of() : pilotGroups;
        }
    }
}
