package dexter.banking.booktransfers.core.application.compliance;

import dexter.banking.booktransfers.core.application.compliance.command.ApproveComplianceCaseCommand;
import dexter.banking.booktransfers.core.application.payment.command.ResumePaymentCommand;
import dexter.banking.booktransfers.core.domain.compliance.ComplianceCase;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.port.in.compliance.ApproveComplianceCaseUseCase;
import dexter.banking.booktransfers.core.port.in.payment.ResumePaymentUseCase;
import dexter.banking.booktransfers.core.port.out.ComplianceCaseRepositoryPort;
import dexter.banking.booktransfers.core.port.out.EventDispatcherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApproveComplianceCaseService implements ApproveComplianceCaseUseCase {
    private final ComplianceCaseRepositoryPort complianceCaseRepository;
    private final EventDispatcherPort eventDispatcher;
    private final ResumePaymentUseCase resumePaymentUseCase;

    @Override
    @Transactional
    public void approve(ApproveComplianceCaseCommand command) {
        ComplianceCase complianceCase = complianceCaseRepository.findById(command.complianceCaseId())
           .orElseThrow(() -> new TransactionNotFoundException("ComplianceCase not found: " + command.complianceCaseId()));

        complianceCase.approve();

        complianceCaseRepository.save(complianceCase);
        // The ComplianceCaseApproved event is still published for auditing or other potential listeners,
        // but our saga no longer acts on it.
        eventDispatcher.dispatch(complianceCase.pullDomainEvents());

        // Directly invoke the next step in the process
        resumePaymentUseCase.resume(new ResumePaymentCommand(complianceCase.getPaymentId()));
    }
}
