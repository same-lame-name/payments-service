package dexter.banking.booktransfers.core.port.in.compliance;

import java.util.UUID;

public interface CreateComplianceCaseUseCase {
    void create(UUID paymentId);
}
