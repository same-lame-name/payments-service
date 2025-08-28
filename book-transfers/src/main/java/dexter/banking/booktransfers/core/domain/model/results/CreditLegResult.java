package dexter.banking.booktransfers.core.domain.model.results;

import dexter.banking.booktransfers.core.domain.primitives.ValueObject;

import java.util.UUID;

/**
 * A pure domain value object representing the outcome of the credit leg operation.
 * It contains only the information relevant to the domain.
 */
public record CreditLegResult(
    UUID creditCardRequestId,
    CreditLegStatus status
) implements ValueObject {

    public enum CreditLegStatus {
        SUCCESSFUL,
        FAILED
    }
}
