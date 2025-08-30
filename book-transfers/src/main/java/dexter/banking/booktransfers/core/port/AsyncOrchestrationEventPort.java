package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;

import java.util.UUID;


/**
 * A dedicated Driven Port for delivering asynchronous event results to the orchestration layer.
 * This adheres to the Interface Segregation Principle, as its methods are only relevant to
 * orchestration adapters that can handle asynchronous, out-of-band events.
 * The port now speaks in the pure language of the domain, using domain-specific value objects.
 */
public interface AsyncOrchestrationEventPort {
    void processCreditLegResult(UUID transactionId, CreditLegResult result);
    void processDebitLegResult(UUID transactionId, DebitLegResult result);
    void processLimitEarmarkResult(UUID transactionId, LimitEarmarkResult result);
}

