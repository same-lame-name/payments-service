package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;

/**
 * Driven Port for the orchestration logic (e.g., the State Machine).
 * The application service will call this port to initiate the complex transaction workflow.
 */
public interface TransactionOrchestratorPort {
    PaymentResult processTransaction(PaymentCommand command, Payment payment);
}
