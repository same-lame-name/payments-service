package dexter.banking.booktransfers.core.application.payment.orchestration.async.model;

/**
 * Defines the events that can trigger state transitions in the orchestration process.
 * This is an application-level concept, distinct from the domain's events.
 */
public enum AsyncProcessEvent {
    SUBMIT,

    // Action Outcome Observations
    LIMIT_EARMARK_SUCCEEDED,
    LIMIT_EARMARK_FAILED,

    DEBIT_LEG_SUCCEEDED,
    DEBIT_LEG_FAILED,

    CREDIT_LEG_SUCCEEDED,
    CREDIT_LEG_FAILED,

    DEBIT_LEG_REVERSAL_SUCCEEDED,
    DEBIT_LEG_REVERSAL_FAILED,

    LIMIT_EARMARK_REVERSAL_SUCCEEDED,
    LIMIT_EARMARK_REVERSAL_FAILED,
}
