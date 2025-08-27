package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.PaymentCommand;
import dexter.banking.booktransfers.core.domain.model.PaymentResponse;

/**
 * Driven Port for the orchestration logic (e.g., the State Machine).
 * The application service will call this port to initiate the complex transaction workflow.
 */
public interface TransactionOrchestratorPort {
    PaymentResponse processTransaction(PaymentCommand command);
}


