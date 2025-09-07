package dexter.banking.booktransfers.core.domain.payment.valueobject.result;
import dexter.banking.booktransfers.core.domain.shared.primitives.ValueObject;

import java.util.UUID;

/**
 * A pure domain value object representing the outcome of the debit leg operation or its reversal.
 * It contains only the information relevant to the domain.
 */
public record DebitLegResult(
        UUID depositId,
        DebitLegStatus status
) implements ValueObject {

    public enum DebitLegStatus {
        SUCCESSFUL,
        FAILED,
        REVERSAL_SUCCESSFUL,
        REVERSAL_FAILED
    }
}
