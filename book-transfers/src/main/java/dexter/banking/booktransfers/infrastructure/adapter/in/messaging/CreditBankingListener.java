package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.usecase.payment.event.RecordCreditResultCommand;
import dexter.banking.booktransfers.infrastructure.adapter.in.messaging.mapper.MessagingAdapterMapper;
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
public class CreditBankingListener {

    private final CommandBus commandBus;
    private final MessagingAdapterMapper mapper;

    @JmsListener(destination = JmsConstants.CREDIT_CARD_BANKING_RESPONSE)
    public void completeCreditCardBanking(CreditCardBankingResponse result) {
        log.debug("Received Credit card banking Result DTO for txnId: {}. Translating to command.", result.getTransactionId());
        var domainResult = mapper.toDomain(result);
        var command = new RecordCreditResultCommand(result.getTransactionId(), domainResult);
        command.execute(commandBus);
    }
}
