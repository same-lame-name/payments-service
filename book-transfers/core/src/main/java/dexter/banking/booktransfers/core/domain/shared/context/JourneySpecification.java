package dexter.banking.booktransfers.core.domain.shared.context;

import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.Map;

/**
 * A pure, technology-agnostic value object representing the complete, unified
 * configuration for a single business journey.
 * This is the carrier for the materialized blueprint proxy and its raw configuration.
 */
@Getter
@RequiredArgsConstructor
public class JourneySpecification {
    private final String journeyName;
    private final JourneyType journeyType;
    private final JourneyBlueprint blueprint;
}
