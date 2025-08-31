package dexter.banking.booktransfers.core.usecase.payment.orchestration.async.model;

/**
 * Defines the possible states a transaction orchestration can be in during its lifecycle.
 * This is an application-level concept, representing workflow steps, and is
 * completely separate from the domain's PaymentState.
 */
public enum AsyncProcessState {
    NEW,
    EARMARKING_LIMIT,
    DEBITING_FUNDS,
    CREDITING_FUNDS,
    REVERSING_DEBIT,
    REVERSING_LIMIT_EARMARK,

    // Terminal States
    REMEDIATION_REQUIRED,
    PROCESS_FAILED,
    PROCESS_SETTLED,
}
