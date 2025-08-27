package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.PaymentResponse;

import java.util.Optional;
import java.util.UUID;

/**
 * Driven Port for transaction persistence. This is the contract for the database adapter.
 * It uses the pure domain model and is completely independent of the underlying database technology (e.g., MongoDB).
 */
public interface TransactionRepository {
    PaymentResponse save(PaymentResponse paymentResponse);
    PaymentResponse update(PaymentResponse paymentResponse);
    Optional<PaymentResponse> findByTransactionId(UUID transactionId);
}


