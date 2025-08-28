package dexter.banking.booktransfers.infrastructure.adapter.out.config;

import lombok.Data;

@Data
public class CommandConfig {
    private boolean idempotencyApplicable = false; // Safe default
    private String orchestrator;
}
