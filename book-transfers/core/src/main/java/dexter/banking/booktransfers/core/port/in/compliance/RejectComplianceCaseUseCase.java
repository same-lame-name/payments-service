package dexter.banking.booktransfers.core.port.in.compliance;

import dexter.banking.booktransfers.core.application.compliance.command.RejectComplianceCaseCommand;

public interface RejectComplianceCaseUseCase {
    void reject(RejectComplianceCaseCommand command);
}
