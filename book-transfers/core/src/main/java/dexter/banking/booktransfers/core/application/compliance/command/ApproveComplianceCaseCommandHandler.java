package dexter.banking.booktransfers.core.application.compliance.command;

import dexter.banking.booktransfers.core.port.in.compliance.ApproveComplianceCaseUseCase;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ApproveComplianceCaseCommandHandler implements CommandHandler<ApproveComplianceCaseCommand, Void> {
    private final ApproveComplianceCaseUseCase approveComplianceCaseUseCase;

    @Override
    @Transactional
    public Void handle(ApproveComplianceCaseCommand command) {
        this.approveComplianceCaseUseCase.approve(command);
        return null;
    }
}
