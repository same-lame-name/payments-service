package dexter.banking.booktransfers.core.port.in.payment;

import dexter.banking.booktransfers.core.application.payment.query.PaymentView;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The dedicated Driving Port for all payment query operations.
 * Its contract is defined by the lean, optimized PaymentView read model,
 * completely decoupling the query path from the write model.
 */
public interface PaymentQueryUseCase {

    Optional<PaymentView> findById(UUID transactionId);

    /**
     * Finds transactions by their business-level reference.
     * As the reference is not guaranteed to be unique, this must return a list.
     * @param transactionReference The business reference identifier.
     * @return A list of matching payment views, which may be empty.
     */
    List<PaymentView> findByReference(String transactionReference);
}
