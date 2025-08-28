package dexter.banking.booktransfers.infrastructure.adapter.out.messaging.transaction.jms;

import dexter.banking.booktransfers.core.port.TransactionLegPort;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.model.JmsConstants;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

/**
 * A Driven Adapter that implements the MessagingPort for sending outbound JMS messages.
 * This class encapsulates all the logic and dependencies related to JMS,
 * hiding the JmsTemplate from the application core.
 */
@Component
@RequiredArgsConstructor
public class JmsTransactionLegAdapter implements TransactionLegPort {

    private final JmsTemplate jmsTemplate;

    @Override
    public void sendCreditCardRequest(CreditCardBankingRequest request) {
        jmsTemplate.convertAndSend(JmsConstants.CREDIT_CARD_BANKING_REQUEST, request);
    }

    @Override
    public void sendDepositRequest(DepositBankingRequest request) {
        jmsTemplate.convertAndSend(JmsConstants.DEPOSIT_BANKING_REQUEST, request);
    }

    @Override
    public void sendLimitManagementRequest(LimitManagementRequest request) {
        jmsTemplate.convertAndSend(JmsConstants.LIMIT_MANAGEMENT_REQUEST, request);
    }

    @Override
    public void sendDepositReversalRequest(DepositBankingReversalRequest request) {
        jmsTemplate.convertAndSend(JmsConstants.DEPOSIT_BANKING_REVERSAL_REQUEST, request);
    }

    @Override
    public void sendLimitReversalRequest(LimitManagementReversalRequest request) {
        jmsTemplate.convertAndSend(JmsConstants.LIMIT_MANAGEMENT_REVERSAL_REQUEST, request);
    }
}

