package dexter.banking.booktransfers.core.port.out;

/**
 * Driven Port for sending asynchronous messages to external systems (e.g., via JMS/Kafka).
 * This abstracts away the JmsTemplate dependency from the core logic.
 */
public interface TransactionLegPort {
    void sendCreditCardRequest(CreditCardPort.SubmitCreditCardPaymentRequest request);
    void sendDepositRequest(DepositPort.SubmitDepositRequest request);
    void sendLimitManagementRequest(LimitPort.EarmarkLimitRequest request);
    void sendDepositReversalRequest(DepositPort.SubmitDepositReversalRequest request);
    void sendLimitReversalRequest(LimitPort.ReverseLimitEarmarkRequest request);
}
