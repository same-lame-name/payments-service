package dexter.banking.booktransfers.core.application.compliance.command;

import dexter.banking.booktransfers.core.port.in.compliance.RejectComplianceCaseUseCase;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RejectComplianceCaseCommandHandler implements CommandHandler<RejectComplianceCaseCommand, Void> {
    private final RejectComplianceCaseUseCase rejectComplianceCaseUseCase;

    @Override
    @Transactional
    public Void handle(RejectComplianceCaseCommand command) {
        this.rejectComplianceCaseUseCase.reject(command);
        return null;
    }
}
