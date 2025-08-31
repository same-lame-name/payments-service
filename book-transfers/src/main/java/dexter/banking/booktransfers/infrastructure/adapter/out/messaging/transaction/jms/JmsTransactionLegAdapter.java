package dexter.banking.booktransfers.infrastructure.adapter.out.messaging.transaction.jms;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.port.TransactionLegPort;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.model.JmsConstants;
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
    private final JmsAdapterMapper mapper;

    @Override
    public void sendCreditCardRequest(PaymentCommand command) {
        var request = mapper.toCreditCardBankingRequest(command);
        jmsTemplate.convertAndSend(JmsConstants.CREDIT_CARD_BANKING_REQUEST, request);
    }

    @Override
    public void sendDepositRequest(PaymentCommand command) {
        var request = mapper.toDepositBankingRequest(command);
        jmsTemplate.convertAndSend(JmsConstants.DEPOSIT_BANKING_REQUEST, request);
    }

    @Override
    public void sendLimitManagementRequest(PaymentCommand command) {
        var request = mapper.toLimitManagementRequest(command);
        jmsTemplate.convertAndSend(JmsConstants.LIMIT_MANAGEMENT_REQUEST, request);
    }

    @Override
    public void sendDepositReversalRequest(Payment payment) {
        var request = mapper.toDepositReversalRequest(payment);
        jmsTemplate.convertAndSend(JmsConstants.DEPOSIT_BANKING_REVERSAL_REQUEST, request);
    }

    @Override
    public void sendLimitReversalRequest(Payment payment) {
        var request = mapper.toLimitEarmarkReversalRequest(payment);
        jmsTemplate.convertAndSend(JmsConstants.LIMIT_MANAGEMENT_REVERSAL_REQUEST, request);
    }
}
