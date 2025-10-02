package dexter.banking.booktransfers.core.application.payment.command.callback;

import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.CreditLegResult;
import dexter.banking.commandbus.Command;

import java.util.UUID;

/**
 * A dedicated, internal command to process the result of a credit card leg callback.
 */
public record ProcessCreditCardResultCommand(
        UUID transactionId,
        CreditLegResult result
) implements Command<Void> {
    /**
     * The identifier is now dynamic, allowing for version-specific configuration
     * of middleware behaviors like idempotency or orchestration strategy selection.
     */
    @Override
    public String getIdentifier() {
       return "PAYMENT_SUBMIT_V2_ASYNC";
    }
}
