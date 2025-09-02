package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model;

public enum ProcessStateV3 {
    NEW,
    EARMARKING_LIMIT, // Sync
    CHECKING_COMPLIANCE, // Sync (cascade)
    AWAITING_COMPLIANCE_APPROVAL, // PAUSE
    DEBITING_FUNDS, // Async
    CREDITING_FUNDS, // Async
    REVERSING_DEBIT, // Async
    REVERSING_LIMIT_EARMARK, // Sync

    // Terminal States
    REMEDIATION_REQUIRED,
    PROCESS_FAILED,
    PROCESS_SETTLED,
}
