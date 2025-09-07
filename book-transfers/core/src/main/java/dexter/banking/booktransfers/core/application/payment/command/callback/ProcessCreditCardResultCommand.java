package dexter.banking.booktransfers.core.application.payment.command.callback;

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
}
