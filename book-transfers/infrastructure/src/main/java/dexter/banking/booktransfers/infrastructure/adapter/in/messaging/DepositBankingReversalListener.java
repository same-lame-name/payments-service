package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.application.payment.command.AsyncPaymentV2CommandHandler;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.JmsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DepositBankingReversalListener {

    private final AsyncPaymentV2CommandHandler orchestrator;
    private final MessagingAdapterMapper mapper;

    @JmsListener(destination = JmsConstants.DEPOSIT_BANKING_REVERSAL_RESPONSE)
    public void completeDepositBankingReversal(DepositBankingResponse result) {
        log.debug("Received Deposit banking reversal DTO for txnId: {}. Delegating to orchestrator.", result.getTransactionId());
        var domainResult = mapper.toReversalDomain(result);
        orchestrator.processDebitReversalResult(domainResult, result.getTransactionId());
    }
}
