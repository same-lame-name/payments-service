package dexter.banking.booktransfers.core.domain.model.config;

import dexter.banking.booktransfers.core.domain.primitives.ValueObject;

/**
 * A pure, technology-agnostic value object representing the configuration for a command.
 * This is the contract defined by the core that the infrastructure layer must fulfill.
 */
public record CommandConfiguration(
        boolean isIdempotencyEnabled,
        String journeyName
) implements ValueObject {
}
