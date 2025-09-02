package dexter.banking.booktransfers.core.application.compliance;

import dexter.banking.booktransfers.core.domain.payment.event.PaymentRequiresComplianceCheck;
import dexter.banking.booktransfers.core.port.in.compliance.CreateComplianceCaseUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComplianceSaga {
    private final CreateComplianceCaseUseCase createComplianceCaseUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentRequiresComplianceCheck event) {
        log.info("SAGA: Received PaymentRequiresComplianceCheck for paymentId {}. Invoking CreateComplianceCaseUseCase.", event.aggregateId());
        // No command object is needed for this internal-only use case.
        createComplianceCaseUseCase.create(event.aggregateId());
    }
}
