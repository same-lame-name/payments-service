package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.domain.payment.valueobject.result.LimitEarmarkResult;

import java.util.UUID;

/**
 * Driven Port for interacting with the external Limit Management service.
 * The contract is defined in terms of pure domain objects.
 */
public interface LimitPort {
    LimitEarmarkResult earmarkLimit(EarmarkLimitRequest request);
    LimitEarmarkResult reverseLimitEarmark(ReverseLimitEarmarkRequest request);

    record EarmarkLimitRequest(UUID transactionId, String limitType) {}
    record ReverseLimitEarmarkRequest(UUID transactionId, UUID limitManagementId) {}
}
