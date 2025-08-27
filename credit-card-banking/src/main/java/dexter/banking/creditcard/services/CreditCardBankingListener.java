package dexter.banking.creditcard.services;

import dexter.banking.model.JmsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.CreditCardBankingResponse;

@Slf4j
@RequiredArgsConstructor
@Component
public class CreditCardBankingListener {

    private final CreditCardBankingService creditCardBankingService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConstants.CREDIT_CARD_BANKING_REQUEST)
    public void listen(CreditCardBankingRequest creditCardBankingRequest) {
        log.debug("Credit card banking request (JMS): {}", creditCardBankingRequest);
        CreditCardBankingResponse creditCardBankingResponse = creditCardBankingService.submitCreditCardBankingRequest(creditCardBankingRequest);
        jmsTemplate.convertAndSend(JmsConstants.CREDIT_CARD_BANKING_RESPONSE, creditCardBankingResponse);
    }
}
