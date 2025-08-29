package dexter.banking.booktransfers.infrastructure.config;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Represents the configuration for a single payment journey,
 * specifying the sequence of business policy beans to apply.
 */
@Data
public class JourneyConfig {
    @NotEmpty
    private List<String> policies;
}
