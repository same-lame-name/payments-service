package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.application.payment.command.callback.ProcessDebitResultCommand;
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
class DepositBankingListener {

    private final CommandBus commandBus;
    private final MessagingAdapterMapper mapper;

    @JmsListener(destination = JmsConstants.DEPOSIT_BANKING_RESPONSE)
    public void completeDepositBanking(DepositBankingResponse result) {
        log.debug("Received Deposit banking Result DTO for txnId: {}. Sending to CommandBus.", result.getTransactionId());
        var domainResult = mapper.toDomain(result);
        var command = new ProcessDebitResultCommand(result.getTransactionId(), domainResult);
        commandBus.send(command);
    }
}
