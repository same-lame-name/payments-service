package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model;

public enum ProcessEventV3 {
    SUBMIT,

    LIMIT_APPROVED,
    LIMIT_REJECTED,

    COMPLIANCE_PENDING,
    COMPLIANCE_NOT_REQUIRED,
    COMPLIANCE_REJECTED,

    RESUME, // Event from Compliance Saga

    DEBIT_LEG_SUCCEEDED,
    DEBIT_LEG_FAILED,

    CREDIT_LEG_SUCCEEDED,
    CREDIT_LEG_FAILED,

    DEBIT_LEG_REVERSAL_SUCCEEDED,
    DEBIT_LEG_REVERSAL_FAILED,

    LIMIT_EARMARK_REVERSAL_SUCCEEDED,
    LIMIT_EARMARK_REVERSAL_FAILED
}
