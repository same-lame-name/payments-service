package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;

/**
 * Driven Port for interacting with the external Limit Management service.
 * The contract is defined in terms of pure domain objects.
 */
public interface LimitPort {
    LimitEarmarkResult earmarkLimit(PaymentCommand command);
    LimitEarmarkResult reverseLimitEarmark(Payment payment);
}
