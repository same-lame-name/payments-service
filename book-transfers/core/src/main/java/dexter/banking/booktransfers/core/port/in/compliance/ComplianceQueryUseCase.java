package dexter.banking.booktransfers.core.port.in.compliance;

import dexter.banking.booktransfers.core.application.compliance.query.ComplianceCaseView;

import java.util.Optional;
import java.util.UUID;

public interface ComplianceQueryUseCase {
    Optional<ComplianceCaseView> findByPaymentId(UUID paymentId);
}
