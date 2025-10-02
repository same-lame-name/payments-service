package dexter.banking.booktransfers.core.application.payment.command.callback;

import dexter.banking.booktransfers.core.domain.payment.valueobject.result.DebitLegResult;
import dexter.banking.commandbus.AbstractEnrichableCommand;
import lombok.Getter; // For generating getters for final fields
import lombok.RequiredArgsConstructor; // For generating constructor for final fields

import java.util.UUID;

/**
 * A dedicated, internal command to process the result of a debit leg reversal callback.
 */
@RequiredArgsConstructor // Generates constructor for all final fields
@Getter // Generates getters for all final fields
public class ProcessDebitReversalResultCommand extends AbstractEnrichableCommand<Void> { // Extend the abstract base class

    private final UUID transactionId;
    private final DebitLegResult result;

    /**
     * The identifier is now dynamic, allowing for version-specific configuration
     * of middleware behaviors like idempotency or orchestration strategy selection.
     */
    @Override
    public String getIdentifier() {
        return "PAYMENT_SUBMIT_V2_ASYNC_CALLBACK"; // Keep the existing identifier logic
    }
}
