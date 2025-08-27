package dexter.banking.booktransfers.core.domain.model;

import dexter.banking.commandbus.Command;
import dexter.banking.model.DepositBankingResponse;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class ProcessDebitLegResultCommand implements Command<Void> {
    DepositBankingResponse response;

    @Override
    public String getIdentifier() {
        return "PROCESS_DEBIT_LEG";
    }
}
