package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingReversalRequest;
import java.util.UUID;

/**
 * Driven Port for interacting with the external Deposit Banking service.
 * The contract is defined in terms of pure domain objects.
 */
public interface DepositPort {
    DebitLegResult submitDeposit(DepositBankingRequest request);
    DebitLegResult submitDepositReversal(UUID depositRequestId, DepositBankingReversalRequest request);
}
