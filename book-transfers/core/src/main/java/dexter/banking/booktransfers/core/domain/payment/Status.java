package dexter.banking.booktransfers.core.domain.payment;

/**
 * Represents the overall status of a transaction from a high-level business perspective.
 */
public enum Status {
    NEW,
    IN_PROGRESS,
    SUCCESSFUL,
    FAILED,
    REMEDIATION_REQUIRED,
}


