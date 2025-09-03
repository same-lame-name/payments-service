package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.application.payment.command.callback.ProcessCreditCardResultCommand;
import dexter.banking.commandbus.CommandBus;
import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.JmsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class CreditBankingListener {

    private final CommandBus commandBus;
    private final MessagingAdapterMapper mapper;

    @JmsListener(destination = JmsConstants.CREDIT_CARD_BANKING_RESPONSE)
    public void completeCreditCardBanking(CreditCardBankingResponse result) {
        log.debug("Received Credit card banking Result DTO for txnId: {}. Sending to CommandBus.", result.getTransactionId());
        var domainResult = mapper.toDomain(result);
        var command = new ProcessCreditCardResultCommand(result.getTransactionId(), domainResult);
        commandBus.send(command);
    }
}
