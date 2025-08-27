package dexter.banking.booktransfers.core.port;

import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.DepositBankingReversalRequest;
import java.util.UUID;

/**
 * Driven Port for interacting with the external Deposit Banking service.
 */
public interface DepositPort {
    DepositBankingResponse submitDeposit(DepositBankingRequest request);
    DepositBankingResponse submitDepositReversal(UUID depositRequestId, DepositBankingReversalRequest request);
}


