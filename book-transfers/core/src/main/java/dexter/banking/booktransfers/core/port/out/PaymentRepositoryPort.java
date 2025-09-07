package dexter.banking.booktransfers.core.port.out;

import dexter.banking.booktransfers.core.domain.payment.Payment;

import java.util.Optional;
import java.util.UUID;

/**
 * Driven Port for payment persistence. This is the contract for the database adapter.
 */
public interface PaymentRepositoryPort {
    Payment save(Payment payment);
    Payment update(Payment payment);

    /**
     * Finds the raw, persisted state (Memento) of a Payment aggregate.
     * This method intentionally returns a data-only object, not a live domain entity,
     * forcing the Application Layer to be responsible for rehydration.
     *
     * @param transactionId The unique ID of the transaction.
     * @return An Optional containing the memento if found.
     */
    Optional<Payment.PaymentMemento> findMementoById(UUID transactionId);
}
