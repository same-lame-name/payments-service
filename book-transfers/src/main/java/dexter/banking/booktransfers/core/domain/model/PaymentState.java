package dexter.banking.booktransfers.core.domain.model;

/**
 * Represents the business status of the Payment aggregate.
 * This is a pure domain concept, independent of any process or workflow state.
 */
public enum PaymentState {
    NEW,
    PENDING_COMPLIANCE,
    LIMIT_RESERVED,
    LIMIT_COULD_NOT_BE_RESERVED,
    FUNDS_DEBITED,
    FUNDS_COULD_NOT_BE_DEBITED,
    FUNDS_CREDITED,
    FUNDS_COULD_NOT_BE_CREDITED,

    LIMIT_REVERSED,
    LIMIT_COULD_NOT_BE_REVERSED,
    FUNDS_DEBIT_REVERSED,
    FUNDS_DEBIT_COULD_NOT_BE_REVERSED,

    // Terminal State (Requires Manual Intervention)
    SETTLED, // Terminal State (Success)
    FAILED, // Terminal State (Failure)
    REMEDIATION_NEEDED,
}
