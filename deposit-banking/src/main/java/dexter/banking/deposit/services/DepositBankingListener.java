package dexter.banking.deposit.services;

import dexter.banking.model.JmsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingResponse;

@Slf4j
@RequiredArgsConstructor
@Component
public class DepositBankingListener {

    private final DepositBankingService depositBankingService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConstants.DEPOSIT_BANKING_REQUEST)
    public void listen(DepositBankingRequest depositBankingRequest) {
        log.debug("Process deposit banking request (JMS): {}", depositBankingRequest);
        DepositBankingResponse depositBankingResult = depositBankingService.submitDepositBankingRequest(depositBankingRequest);
        jmsTemplate.convertAndSend(JmsConstants.DEPOSIT_BANKING_RESPONSE, depositBankingResult);
    }
}
