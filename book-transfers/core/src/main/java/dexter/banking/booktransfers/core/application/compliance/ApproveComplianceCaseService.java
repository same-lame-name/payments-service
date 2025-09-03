package dexter.banking.booktransfers.core.application.compliance;

import dexter.banking.booktransfers.core.application.compliance.command.ApproveComplianceCaseCommand;
import dexter.banking.booktransfers.core.domain.compliance.ComplianceCase;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.port.in.compliance.ApproveComplianceCaseUseCase;
import dexter.banking.booktransfers.core.port.in.payment.ResumePaymentParams;
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
        eventDispatcher.dispatch(complianceCase.pullDomainEvents());

        resumePaymentUseCase.resume(new ResumePaymentParams(complianceCase.getPaymentId()));
    }
}
