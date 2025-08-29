package dexter.banking.booktransfers.core.domain.model;

/**
 * Represents the business status of the Payment aggregate.
 * This is a pure domain concept, independent of any process or workflow state.
 */
public enum PaymentState {
    NEW,
    PENDING_COMPLIANCE,
    LIMIT_RESERVED,
    FUNDS_DEBITED,
    SETTLED, // Terminal State (Success)
    FAILED, // Terminal State (Failure)
    REMEDIATION_NEEDED // Terminal State (Requires Manual Intervention)
}
