package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * Driven Port for payment persistence. This is the contract for the database adapter.
 * It uses the pure domain model and is completely independent of the underlying database technology (e.g., MongoDB).
 */
public interface PaymentRepositoryPort {
    Payment save(Payment payment);
    Payment update(Payment payment);
    Optional<Payment> findById(UUID transactionId);
}
