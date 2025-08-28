package dexter.banking.booktransfers.core.usecase.event;

import dexter.banking.commandbus.Command;
import dexter.banking.model.CreditCardBankingResponse;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class ProcessCreditLegResultCommand implements Command<Void> {
    CreditCardBankingResponse response;

    @Override
    public String getIdentifier() {
        return "PROCESS_CREDIT_LEG";
    }
}
