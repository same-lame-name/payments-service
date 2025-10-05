package dexter.banking.booktransfers.core.application.payment.command.callback;

import dexter.banking.booktransfers.core.domain.payment.valueobject.result.DebitLegResult;
import dexter.banking.commandbus.AbstractEnrichableCommand;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

/**
 * A dedicated, internal command to process the result of a debit leg callback.
 */
@RequiredArgsConstructor
@Getter
public class ProcessDebitResultCommand extends AbstractEnrichableCommand<Void> { // Extend the abstract base class

    private final UUID transactionId;
    private final DebitLegResult result;

    /**
     * The identifier is now dynamic, allowing for version-specific configuration
     * of middleware behaviors like idempotency or orchestration strategy selection.
     */
    @Override
    public String getIdentifier() {
        return "PAYMENT_SUBMIT_V2_ASYNC_CALLBACK";

    }
}
