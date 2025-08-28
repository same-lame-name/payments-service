package dexter.banking.booktransfers.core.domain.model;

/**
 * Defines the possible states a transaction can be in during its lifecycle.
 * This is a pure domain concept, independent of any state machine framework.
 */
public enum TransactionState {
    NEW,
    SUBMITTED, // Note: This state is now transient in the synchronous model
    LIMIT_EARMARK_IN_PROGRESS,
    LIMIT_EARMARK_COMPLETED,
    LIMIT_EARMARK_REVERSAL_IN_PROGRESS,
    LIMIT_EARMARK_REVERSAL_COMPLETED,
    DEBIT_LEG_IN_PROGRESS,
    DEBIT_LEG_COMPLETED,
    DEBIT_LEG_REVERSAL_IN_PROGRESS,
    DEBIT_LEG_REVERSAL_COMPLETED,
    CREDIT_LEG_IN_PROGRESS,
    CREDIT_LEG_COMPLETED,

    // Terminal States
    MANUAL_INTERVENTION_REQUIRED,
    TRANSACTION_FAILED,
    CREDIT_LEG_FAILED, DEBIT_LEG_FAILED, TRANSACTION_SUCCESSFUL
}

