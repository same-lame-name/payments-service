package dexter.banking.booktransfers.core.domain.shared.context;

import dexter.banking.booktransfers.core.domain.shared.primitives.ValueObject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public record JourneySpecificationDeprecated(
        boolean isIdempotencyEnabled,
        List<String> policies,
        Optional<BigDecimal> complianceThreshold
) implements ValueObject {
    /**
     * Provides a safe, default specification when one is not explicitly configured.
     */
    public static JourneySpecificationDeprecated defaultInstance() {
        return new JourneySpecificationDeprecated(false, Collections.emptyList(), Optional.empty());
    }
}

