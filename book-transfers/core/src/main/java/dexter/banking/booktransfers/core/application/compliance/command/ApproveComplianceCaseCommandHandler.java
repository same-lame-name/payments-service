package dexter.banking.booktransfers.core.application.compliance.command;

import dexter.banking.booktransfers.core.domain.compliance.ComplianceCase;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.port.in.compliance.ApproveComplianceCaseUseCase;
import dexter.banking.booktransfers.core.port.out.ComplianceCaseRepositoryPort;
import dexter.banking.booktransfers.core.port.out.EventDispatcherPort;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ApproveComplianceCaseCommandHandler implements CommandHandler<ApproveComplianceCaseCommand, Void> {
    private final ComplianceCaseRepositoryPort complianceCaseRepository;
    private final EventDispatcherPort eventDispatcher;
    @Override
    @Transactional
    public Void handle(ApproveComplianceCaseCommand command) {
        ComplianceCase complianceCase = complianceCaseRepository.findById(command.complianceCaseId())
                .orElseThrow(() -> new TransactionNotFoundException("ComplianceCase not found: " + command.complianceCaseId()));
        complianceCase.approve();

        complianceCaseRepository.save(complianceCase);
        eventDispatcher.dispatch(complianceCase.pullDomainEvents());
        return null;
    }
}
