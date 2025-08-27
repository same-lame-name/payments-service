package dexter.banking.booktransfers.core.domain.model;

/**
 * Defines the events that can trigger state transitions in a transaction's lifecycle.
 * This is a pure domain concept, independent of any state machine framework.
 */
public enum TransactionEvent {
    // The single external trigger to start the flow
    SUBMIT,

    // --- Action Outcome Observations ---
    LIMIT_EARMARK_SUCCEEDED,
    LIMIT_EARMARK_FAILED,

    DEBIT_LEG_SUCCEEDED,
    DEBIT_LEG_FAILED,

    CREDIT_LEG_SUCCEEDED,
    CREDIT_LEG_FAILED,

    DEBIT_LEG_REVERSAL_SUCCEEDED,
    DEBIT_LEG_REVERSAL_FAILED,

    LIMIT_EARMARK_REVERSAL_SUCCEEDED,
    LIMIT_EARMARK_REVERSAL_FAILED
}


