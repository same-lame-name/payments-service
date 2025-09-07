package dexter.banking.booktransfers.core.application.compliance.command;

import dexter.banking.commandbus.Command;

import java.util.UUID;

public record RejectComplianceCaseCommand(
        UUID complianceCaseId,
        String reason
) implements Command<Void> {
}
