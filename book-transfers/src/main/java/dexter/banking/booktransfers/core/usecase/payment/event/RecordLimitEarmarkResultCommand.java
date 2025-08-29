package dexter.banking.booktransfers.core.usecase.payment.event;

import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.commandbus.Command;
import lombok.Value;

import java.util.UUID;

@Value
public class RecordLimitEarmarkResultCommand implements Command<Void> {
    UUID transactionId;
    LimitEarmarkResult result;

    @Override
    public String getIdentifier() {
        return "RecordLimitEarmarkResult";
    }
}
