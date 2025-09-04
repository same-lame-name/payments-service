package dexter.banking.booktransfers.infrastructure.adapter.out.events.compliance;

import dexter.banking.booktransfers.core.domain.compliance.event.ComplianceCaseApproved;
import dexter.banking.booktransfers.core.domain.compliance.event.ComplianceCaseRejected;
import dexter.banking.booktransfers.core.domain.payment.PaymentState;
import dexter.banking.booktransfers.core.domain.payment.event.*;
import dexter.banking.booktransfers.core.port.in.compliance.CreateComplianceCaseUseCase;
import dexter.banking.booktransfers.core.port.in.payment.FailPaymentParams;
import dexter.banking.booktransfers.core.port.in.payment.FailPaymentUseCase;
import dexter.banking.booktransfers.core.port.in.payment.ResumePaymentParams;
import dexter.banking.booktransfers.core.port.in.payment.ResumePaymentUseCase;
import dexter.banking.booktransfers.core.port.out.WebhookPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

/**
 * A single, unified Spring component that listens for all domain events.
 * It uses the @TransactionalEventListener to ensure that event handling only occurs
 * AFTER the originating transaction has successfully committed. This is critical for
 * preventing inconsistent state if a webhook call fails after the DB commit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class TransactionalComplianceDomainEventListener {
    private final ResumePaymentUseCase resumePaymentUseCase;
    private final FailPaymentUseCase failPaymentUseCase;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ComplianceCaseApproved event) {
        log.info("SAGA: Received PaymentRequiresComplianceCheck for paymentId {}. Invoking CreateComplianceCaseUseCase.", event.aggregateId());

        resumePaymentUseCase.resume(new ResumePaymentParams(event.paymentId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ComplianceCaseRejected event) {
        log.info("SAGA: Received PaymentRequiresComplianceCheck for paymentId {}. Invoking CreateComplianceCaseUseCase.", event.aggregateId());

        failPaymentUseCase.fail(new FailPaymentParams(event.paymentId(), event.reason()));
    }
}
