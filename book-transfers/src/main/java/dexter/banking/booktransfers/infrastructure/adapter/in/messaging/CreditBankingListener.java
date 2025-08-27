package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.domain.model.ProcessCreditLegResultCommand;
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

    @JmsListener(destination = JmsConstants.CREDIT_CARD_BANKING_RESPONSE)
    public void completeCreditCardBanking(CreditCardBankingResponse result) {
        log.debug("Received Credit card banking Result for txnId: {}. Translating to command.", result.getTransactionId());
        var command = new ProcessCreditLegResultCommand(result);
        command.execute(commandBus);
    }
}
