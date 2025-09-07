package dexter.banking.booktransfers.infrastructure.adapter.out.messaging.payment.jms;

import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.booktransfers.core.port.out.TransactionLegPort;
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
class JmsTransactionLegAdapter implements TransactionLegPort {

    private final JmsTemplate jmsTemplate;
    private final JmsAdapterMapper mapper;
    @Override
    public void sendCreditCardRequest(CreditCardPort.SubmitCreditCardPaymentRequest request) {
        var dto = mapper.toCreditCardBankingRequest(request);
        jmsTemplate.convertAndSend(JmsConstants.CREDIT_CARD_BANKING_REQUEST, dto);
    }

    @Override
    public void sendDepositRequest(DepositPort.SubmitDepositRequest request) {
        var dto = mapper.toDepositBankingRequest(request);
        jmsTemplate.convertAndSend(JmsConstants.DEPOSIT_BANKING_REQUEST, dto);
    }

    @Override
    public void sendLimitManagementRequest(LimitPort.EarmarkLimitRequest request) {
        var dto = mapper.toLimitManagementRequest(request);
        jmsTemplate.convertAndSend(JmsConstants.LIMIT_MANAGEMENT_REQUEST, dto);
    }

    @Override
    public void sendDepositReversalRequest(DepositPort.SubmitDepositReversalRequest request) {
        var dto = mapper.toDepositReversalRequest(request);
        jmsTemplate.convertAndSend(JmsConstants.DEPOSIT_BANKING_REVERSAL_REQUEST, dto);
    }

    @Override
    public void sendLimitReversalRequest(LimitPort.ReverseLimitEarmarkRequest request) {
        var dto = mapper.toLimitEarmarkReversalRequest(request);
        jmsTemplate.convertAndSend(JmsConstants.LIMIT_MANAGEMENT_REVERSAL_REQUEST, dto);
    }
}
