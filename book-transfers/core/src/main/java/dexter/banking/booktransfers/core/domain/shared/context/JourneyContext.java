package dexter.banking.booktransfers.core.domain.shared.context;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * An immutable class that holds the JourneySpecification for the currently executing journey.
 * This serves as the carrier for all journey-specific configuration.
 */
@Getter
@RequiredArgsConstructor
public class JourneyContext {
    private final JourneySpecification specification;
}
