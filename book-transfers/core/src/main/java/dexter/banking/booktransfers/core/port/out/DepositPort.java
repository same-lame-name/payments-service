package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.domain.payment.valueobject.result.DebitLegResult;

import java.util.UUID;

/**
 * Driven Port for interacting with the external Deposit Banking service.
 * The contract is defined in terms of pure domain objects.
 */
public interface DepositPort {
    DebitLegResult submitDeposit(SubmitDepositRequest request);
    DebitLegResult submitDepositReversal(SubmitDepositReversalRequest request);

    record SubmitDepositRequest(UUID transactionId, String accountNumber) {}
    record SubmitDepositReversalRequest(UUID transactionId, UUID reservationId) {}
}
