package dexter.banking.booktransfers.core.domain.payment.result;


import dexter.banking.booktransfers.core.domain.shared.primitives.ValueObject;

import java.util.UUID;

/**
 * A pure domain value object representing the outcome of the limit earmark operation or its reversal.
 * It contains only the information relevant to the domain.
 */
public record LimitEarmarkResult(
    UUID limitId,
    LimitEarmarkStatus status
) implements ValueObject {

    public enum LimitEarmarkStatus {
        SUCCESSFUL,
        FAILED,
        REVERSAL_SUCCESSFUL,
        REVERSAL_FAILED
    }
}
