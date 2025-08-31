package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;

/**
 * Driven Port for interacting with the external Credit Card Banking service.
 * The contract is defined in terms of pure domain objects, protecting the core from
 * the specifics of the external DTOs.
 */
public interface CreditCardPort {
    CreditLegResult submitCreditCardPayment(PaymentCommand command);
    CreditLegResult submitCreditCardReversalPayment(Payment payment);
}
