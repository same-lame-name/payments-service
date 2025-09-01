package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.application.payment.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.result.LimitEarmarkResult;

/**
 * Driven Port for interacting with the external Limit Management service.
 * The contract is defined in terms of pure domain objects.
 */
public interface LimitPort {
    LimitEarmarkResult earmarkLimit(PaymentCommand command);
    LimitEarmarkResult reverseLimitEarmark(Payment payment);
}
