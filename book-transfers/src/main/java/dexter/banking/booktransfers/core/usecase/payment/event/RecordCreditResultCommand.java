package dexter.banking.booktransfers.core.usecase.payment.event;

import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.commandbus.Command;
import lombok.Value;

import java.util.UUID;

@Value
public class RecordCreditResultCommand implements Command<Void> {
    UUID transactionId;
    CreditLegResult result;

    @Override
    public String getIdentifier() {
        return "RecordCreditResult";
    }
}
