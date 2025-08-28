package dexter.banking.booktransfers.core.usecase.event;

import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.commandbus.Command;
import lombok.RequiredArgsConstructor;
import lombok.Value;

import java.util.UUID;

@Value
@RequiredArgsConstructor
public class ProcessCreditLegResultCommand implements Command<Void> {
    // The command now carries the pure domain value object, not the external DTO.
    UUID transactionId;
    CreditLegResult result;

    @Override
    public String getIdentifier() {
        return "PROCESS_CREDIT_LEG";
    }
}
