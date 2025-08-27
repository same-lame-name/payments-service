package dexter.banking.booktransfers.core.port;

import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementResponse;
import dexter.banking.model.LimitManagementReversalRequest;
import java.util.UUID;

/**
 * Driven Port for interacting with the external Limit Management service.
 */
public interface LimitPort {
    LimitManagementResponse earmarkLimit(LimitManagementRequest request);
    LimitManagementResponse reverseLimitEarmark(UUID limitEarmarkId, LimitManagementReversalRequest request);
}


