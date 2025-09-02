package dexter.banking.booktransfers.core.port.out;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;

/**
 * Driven Port for the orchestration logic (e.g., the State Machine).
 * The application service will call this port to initiate the complex transaction workflow.
 */
public interface TransactionOrchestratorPort {
    PaymentResult processTransaction(PaymentCommand command, Payment payment);
}
