package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import java.util.UUID;

/**
 * Driven Port for interacting with the external Limit Management service.
 * The contract is defined in terms of pure domain objects.
 */
public interface LimitPort {
    LimitEarmarkResult earmarkLimit(LimitManagementRequest request);
    LimitEarmarkResult reverseLimitEarmark(UUID limitEarmarkId, LimitManagementReversalRequest request);
}
