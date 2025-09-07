package dexter.banking.booktransfers.infrastructure.adapter.out.config;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import java.math.BigDecimal;
import java.util.List;

/**
 * A single, unified DTO representing the configuration for a business journey,
 * loaded from application.yml.
 */
@Data
class JourneyProperties {
    private boolean idempotencyEnabled = false;
    @NotEmpty
    private List<String> policies;
    private BigDecimal complianceThreshold;
}
