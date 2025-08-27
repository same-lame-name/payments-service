package dexter.banking.deposit.services;

import dexter.banking.model.JmsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.model.DepositBankingResponse;

@Slf4j
@RequiredArgsConstructor
@Component
public class DepositBankingReversalListener {

    private final DepositBankingService depositBankingService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConstants.DEPOSIT_BANKING_REVERSAL_REQUEST)
    public void listen(DepositBankingReversalRequest depositBankingReversalRequest) {
        log.debug("Deposit banking reversal request (JMS): {}", depositBankingReversalRequest);
        DepositBankingResponse depositBankingReversalResponse = depositBankingService.submitDepositBankingReversal(depositBankingReversalRequest);
        jmsTemplate.convertAndSend(JmsConstants.DEPOSIT_BANKING_REVERSAL_RESPONSE, depositBankingReversalResponse);
    }
}
