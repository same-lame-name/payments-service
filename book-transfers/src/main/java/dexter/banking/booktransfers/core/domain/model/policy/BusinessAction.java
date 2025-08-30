package dexter.banking.booktransfers.core.domain.model.policy;

/**
 * A type-safe representation of a business action being attempted on an aggregate.
 * This is used by policies to select the correct set of rules to evaluate.
 */
public enum BusinessAction {
    START_PAYMENT,
    RECORD_LIMIT_EARMARK_SUCCESS,
    RECORD_LIMIT_EARMARK_FAILURE,
    RECORD_DEBIT_SUCCESS,
    RECORD_DEBIT_FAILURE,
    RECORD_CREDIT_SUCCESS,
    RECORD_CREDIT_FAILURE,
    RECORD_DEBIT_REVERSAL_SUCCESS,
    RECORD_DEBIT_REVERSAL_FAILURE,
    RECORD_LIMIT_REVERSAL_SUCCESS,
    RECORD_LIMIT_REVERSAL_FAILURE,
    RECORD_PAYMENT_SETTLED,
    RECORD_PAYMENT_FAILED,
    RECORD_REMEDIATION_NEEDED
}
