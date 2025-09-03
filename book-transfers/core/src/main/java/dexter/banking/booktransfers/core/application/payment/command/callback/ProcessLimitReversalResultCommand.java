package dexter.banking.booktransfers.core.application.payment.command.callback;

import dexter.banking.booktransfers.core.domain.payment.valueobject.result.LimitEarmarkResult;
import dexter.banking.commandbus.Command;

import java.util.UUID;

/**
 * A dedicated, internal command to process the result of a limit earmark reversal callback.
 */
public record ProcessLimitReversalResultCommand(
        UUID transactionId,
        LimitEarmarkResult result
) implements Command<Void> {
}
