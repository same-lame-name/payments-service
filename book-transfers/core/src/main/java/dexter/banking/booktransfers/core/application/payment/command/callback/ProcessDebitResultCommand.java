package dexter.banking.booktransfers.core.application.payment.command.callback;

import dexter.banking.booktransfers.core.domain.payment.valueobject.result.DebitLegResult;
import dexter.banking.commandbus.Command;

import java.util.UUID;

/**
 * A dedicated, internal command to process the result of a debit leg callback.
 */
public record ProcessDebitResultCommand(
        UUID transactionId,
        DebitLegResult result
) implements Command<Void> {
}
