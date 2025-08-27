package dexter.banking.booktransfers.core.port;

import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.LimitManagementResponse;

/**
 * A dedicated Driven Port for delivering asynchronous event results to the orchestration layer.
 * This adheres to the Interface Segregation Principle, as its methods are only relevant to
 * orchestration adapters that can handle asynchronous, out-of-band events.
 */
public interface AsyncOrchestrationEventPort {
    void processCreditLegResult(CreditCardBankingResponse response);
    void processDebitLegResult(DepositBankingResponse response);
    void processLimitEarmarkResult(LimitManagementResponse response);
    void processDebitLegReversalResult(DepositBankingResponse response);
    void processLimitEarmarkReversalResult(LimitManagementResponse response);
}
