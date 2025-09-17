package dexter.banking.booktransfers.core.domain.shared.context;


import dexter.banking.booktransfers.core.domain.shared.primitives.ValueObject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A pure, technology-agnostic value object representing the complete, unified
 * configuration for a single business journey.
 * This is the contract defined by the
 * core that the infrastructure layer must fulfill.
 */
public record JourneySpecification(
        boolean isIdempotencyEnabled,
        List<String> policies,
        Optional<BigDecimal> complianceThreshold
) implements ValueObject {
    /**
     * Provides a safe, default specification when one is not explicitly configured.
     */
    public static JourneySpecification defaultInstance() {
        return new JourneySpecification(false, Collections.emptyList(), Optional.empty());
    }
}
