package dexter.banking.booktransfers.core.port.out;

import dexter.banking.booktransfers.core.domain.compliance.ComplianceCase;
import java.util.Optional;
import java.util.UUID;

public interface ComplianceCaseRepositoryPort {
    void save(ComplianceCase complianceCase);
    Optional<ComplianceCase> findById(UUID complianceCaseId);
    Optional<ComplianceCase> findByPaymentId(UUID paymentId);
}
