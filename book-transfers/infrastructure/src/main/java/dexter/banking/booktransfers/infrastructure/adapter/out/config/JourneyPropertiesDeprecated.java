package dexter.banking.booktransfers.infrastructure.adapter.out.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * A single, unified DTO representing the configuration for a business journey,
 * loaded from application.yml.
 */
@Data
class JourneyPropertiesDeprecated {
    private boolean idempotencyEnabled = false;
    @NotEmpty
    private List<String> policies;
    private BigDecimal complianceThreshold;
}

