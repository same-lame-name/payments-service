package dexter.banking.booktransfers.core.usecase.event;

import dexter.banking.commandbus.Command;
import dexter.banking.model.DepositBankingResponse;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class ProcessDebitLegReversalResultCommand implements Command<Void> {
    DepositBankingResponse response;

    @Override
    public String getIdentifier() {
        return "PROCESS_DEBIT_LEG_REVERSAL";
    }
}
