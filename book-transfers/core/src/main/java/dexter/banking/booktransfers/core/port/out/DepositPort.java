package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.application.payment.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.result.DebitLegResult;

/**
 * Driven Port for interacting with the external Deposit Banking service.
 * The contract is defined in terms of pure domain objects.
 */
public interface DepositPort {
    DebitLegResult submitDeposit(PaymentCommand command);
    DebitLegResult submitDepositReversal(Payment payment);
}
