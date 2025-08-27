package dexter.banking.booktransfers.infrastructure.config.model;

import lombok.Data;

@Data
public class CommandConfig {
    private boolean idempotencyApplicable = false; // Safe default
    private String orchestrator;
}
