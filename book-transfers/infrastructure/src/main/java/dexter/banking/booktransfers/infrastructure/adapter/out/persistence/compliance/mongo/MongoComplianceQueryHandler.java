package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.compliance.mongo;

import dexter.banking.booktransfers.core.application.compliance.query.ComplianceCaseView;
import dexter.banking.booktransfers.core.port.in.compliance.ComplianceQueryUseCase;
import dexter.banking.booktransfers.core.port.out.ComplianceCaseRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class MongoComplianceQueryHandler implements ComplianceQueryUseCase {

    private final ComplianceCaseRepositoryPort complianceCaseRepository;

    @Override
    public Optional<ComplianceCaseView> findByPaymentId(UUID paymentId) {
        return complianceCaseRepository.findByPaymentId(paymentId)
                .map(complianceCase -> new ComplianceCaseView(
                        complianceCase.getId(),
                        complianceCase.getPaymentId(),
                        complianceCase.getStatus()
                ));
    }
}
