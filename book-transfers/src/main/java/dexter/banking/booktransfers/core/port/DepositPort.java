package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;

/**
 * Driven Port for interacting with the external Deposit Banking service.
 * The contract is defined in terms of pure domain objects.
 */
public interface DepositPort {
    DebitLegResult submitDeposit(PaymentCommand command);
    DebitLegResult submitDepositReversal(Payment payment);
}
