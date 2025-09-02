package dexter.banking.booktransfers.core.application.compliance;

import dexter.banking.booktransfers.core.domain.compliance.ComplianceCase;
import dexter.banking.booktransfers.core.port.in.compliance.CreateComplianceCaseUseCase;
import dexter.banking.booktransfers.core.port.out.ComplianceCaseRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CreateComplianceCaseService implements CreateComplianceCaseUseCase {
    private final ComplianceCaseRepositoryPort complianceCaseRepository;

    @Override
    @Transactional
    public void create(UUID paymentId) {
        // This service's sole responsibility is to create the case in its initial state.
        ComplianceCase complianceCase = ComplianceCase.create(paymentId);
        complianceCaseRepository.save(complianceCase);
    }
}
