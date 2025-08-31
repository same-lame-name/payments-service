package dexter.banking.booktransfers.core.domain.model.config;

import dexter.banking.booktransfers.core.domain.primitives.ValueObject;

import java.util.List;

/**
 * A pure, technology-agnostic value object representing the complete, unified
 * configuration for a single business journey. This is the contract defined by the
 * core that the infrastructure layer must fulfill.
 */
public record JourneySpecification(
        boolean isIdempotencyEnabled,
        List<String> policies
) implements ValueObject {
}
