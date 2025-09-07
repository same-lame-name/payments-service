package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.application.payment.command.callback.ProcessDebitReversalResultCommand;
import dexter.banking.commandbus.CommandBus;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.JmsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class DepositBankingReversalListener {

    private final CommandBus commandBus;
    private final MessagingAdapterMapper mapper;

    @JmsListener(destination = JmsConstants.DEPOSIT_BANKING_REVERSAL_RESPONSE)
    public void completeDepositBankingReversal(DepositBankingResponse result) {
        log.debug("Received Deposit banking reversal DTO for txnId: {}. Sending to CommandBus.", result.getTransactionId());
        var domainResult = mapper.toReversalDomain(result);
        var command = new ProcessDebitReversalResultCommand(result.getTransactionId(), domainResult);
        commandBus.send(command);
    }
}
