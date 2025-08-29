package dexter.banking.booktransfers.infrastructure.config;

import lombok.Data;

@Data
public class CommandConfig {
    private boolean idempotencyApplicable = false;
    private String journeyName; // Maps a command to its business process journey
}
