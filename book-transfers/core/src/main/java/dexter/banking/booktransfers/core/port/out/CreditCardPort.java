package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.CreditLegResult;

/**
 * Driven Port for interacting with the external Credit Card Banking service.
 * The contract is defined in terms of pure domain objects, protecting the core from
 * the specifics of the external DTOs.
 */
public interface CreditCardPort {
    CreditLegResult submitCreditCardPayment(PaymentCommand command);
    CreditLegResult submitCreditCardReversalPayment(Payment payment);
}
