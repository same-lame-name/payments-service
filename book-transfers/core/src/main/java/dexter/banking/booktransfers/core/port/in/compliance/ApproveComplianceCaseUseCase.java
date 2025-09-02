package dexter.banking.booktransfers.core.port.in.compliance;

import dexter.banking.booktransfers.core.application.compliance.command.ApproveComplianceCaseCommand;

public interface ApproveComplianceCaseUseCase {
    void approve(ApproveComplianceCaseCommand command);
}
