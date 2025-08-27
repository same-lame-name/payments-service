package dexter.banking.creditcard.services;

import dexter.banking.model.JmsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import dexter.banking.model.CreditCardBankingReversalRequest;
import dexter.banking.model.CreditCardBankingResponse;

@Slf4j
@RequiredArgsConstructor
@Component
public class CreditCardBankingReversalListener {

    private final CreditCardBankingService creditCardBankingService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConstants.CREDIT_CARD_BANKING_REVERSAL_REQUEST)
    public void listen(CreditCardBankingReversalRequest creditCardBankingReversalRequest) {
        log.debug("Credit card banking reversal request (JMS): {}", creditCardBankingReversalRequest);
        CreditCardBankingResponse creditCardBankingResponse = creditCardBankingService.creditCardBankingReversal(creditCardBankingReversalRequest);
        jmsTemplate.convertAndSend(JmsConstants.CREDIT_CARD_BANKING_REVERSAL_RESPONSE, creditCardBankingResponse);
    }
}
