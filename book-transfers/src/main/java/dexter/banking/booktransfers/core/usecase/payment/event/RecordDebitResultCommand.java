package dexter.banking.booktransfers.core.usecase.payment.event;

import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.commandbus.Command;
import lombok.Value;

import java.util.UUID;

@Value
public class RecordDebitResultCommand implements Command<Void> {
    UUID transactionId;
    DebitLegResult result;

    @Override
    public String getIdentifier() {
        return "RecordDebitResult";
    }
}
