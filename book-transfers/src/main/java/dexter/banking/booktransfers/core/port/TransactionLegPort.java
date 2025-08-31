package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;

/**
 * Driven Port for sending asynchronous messages to external systems (e.g., via JMS/Kafka).
 * This abstracts away the JmsTemplate dependency from the core logic.
 */
public interface TransactionLegPort {
    void sendCreditCardRequest(PaymentCommand command);
    void sendDepositRequest(PaymentCommand command);
    void sendLimitManagementRequest(PaymentCommand command);
    void sendDepositReversalRequest(Payment payment);
    void sendLimitReversalRequest(Payment payment);
}
