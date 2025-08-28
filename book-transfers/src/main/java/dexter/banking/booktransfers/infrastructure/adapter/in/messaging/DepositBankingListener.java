package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.usecase.event.ProcessDebitLegResultCommand;
import dexter.banking.booktransfers.infrastructure.adapter.in.messaging.mapper.MessagingAdapterMapper;
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
public class DepositBankingListener {

    private final CommandBus commandBus;
    private final MessagingAdapterMapper mapper;

    @JmsListener(destination = JmsConstants.DEPOSIT_BANKING_RESPONSE)
    public void completeDepositBanking(DepositBankingResponse result) {
        log.debug("Received Deposit banking Result DTO for txnId: {}. Translating to command.", result.getTransactionId());
        var domainResult = mapper.toDomain(result);
        var command = new ProcessDebitLegResultCommand(result.getTransactionId(), domainResult);
        command.execute(commandBus);
    }
}
