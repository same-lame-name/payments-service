package dexter.banking.booktransfers.core.domain.model;

/**
 * A pure domain enum representing the possible states of an idempotent operation.
 */
public enum IdempotencyStatus {
    STARTED,
    COMPLETED
}
