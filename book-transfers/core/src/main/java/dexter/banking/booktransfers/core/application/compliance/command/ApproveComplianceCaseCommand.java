package dexter.banking.booktransfers.core.application.compliance.command;

import dexter.banking.commandbus.Command;

import java.util.UUID;

public record ApproveComplianceCaseCommand(
    UUID complianceCaseId
) implements Command<Void> {
}
