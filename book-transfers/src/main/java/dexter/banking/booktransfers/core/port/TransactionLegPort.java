package dexter.banking.booktransfers.core.port;

import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementReversalRequest;

/**
 * Driven Port for sending asynchronous messages to external systems (e.g., via JMS/Kafka).
 * This abstracts away the JmsTemplate dependency from the core logic.
 */
public interface TransactionLegPort {
    void sendCreditCardRequest(CreditCardBankingRequest request);
    void sendDepositRequest(DepositBankingRequest request);
    void sendLimitManagementRequest(LimitManagementRequest request);
    void sendDepositReversalRequest(DepositBankingReversalRequest request);
    void sendLimitReversalRequest(LimitManagementReversalRequest request);
}


