package dexter.banking.booktransfers.core.domain.shared.context;

import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.primitives.ValueObject;

/**
 * An immutable record that holds the JourneySpecification for the currently executing journey.
 * This serves as the carrier for all journey-specific configuration.
 */
public record JourneyContext(JourneySpecification specification) implements ValueObject {
}
