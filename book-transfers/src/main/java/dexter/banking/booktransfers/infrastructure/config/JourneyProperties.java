package dexter.banking.booktransfers.infrastructure.config;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * A single, unified DTO representing the configuration for a business journey,
 * loaded from application.yml.
 */
@Data
public class JourneyProperties {
    private boolean idempotencyEnabled = false;
    @NotEmpty
    private List<String> policies;
}
