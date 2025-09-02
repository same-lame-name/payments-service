package dexter.banking.booktransfers.core.application.compliance;

import dexter.banking.booktransfers.core.application.compliance.command.RejectComplianceCaseCommand;
import dexter.banking.booktransfers.core.application.payment.command.FailPaymentCommand;
import dexter.banking.booktransfers.core.domain.compliance.ComplianceCase;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.port.in.compliance.RejectComplianceCaseUseCase;
import dexter.banking.booktransfers.core.port.in.payment.FailPaymentUseCase;
import dexter.banking.booktransfers.core.port.out.ComplianceCaseRepositoryPort;
import dexter.banking.booktransfers.core.port.out.EventDispatcherPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RejectComplianceCaseService implements RejectComplianceCaseUseCase {
    private final ComplianceCaseRepositoryPort complianceCaseRepository;
    private final EventDispatcherPort eventDispatcher;
    private final FailPaymentUseCase failPaymentUseCase;

    @Override
    @Transactional
    public void reject(RejectComplianceCaseCommand command) {
        ComplianceCase complianceCase = complianceCaseRepository.findById(command.complianceCaseId())
                .orElseThrow(() -> new TransactionNotFoundException("ComplianceCase not found: " + command.complianceCaseId()));

        complianceCase.reject(command.reason());

        complianceCaseRepository.save(complianceCase);
        eventDispatcher.dispatch(complianceCase.pullDomainEvents());

        // Directly invoke the next step in the process (failure)
        failPaymentUseCase.fail(new FailPaymentCommand(complianceCase.getPaymentId(), "Compliance rejected: " + command.reason()));
    }
}
