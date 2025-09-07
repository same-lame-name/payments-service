package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.domain.shared.idempotency.IdempotencyData;

import java.util.Optional;
import java.util.UUID;

/**
 * A Driven Port defining the contract for a technology-agnostic idempotency store.
 * The contract is based on explicit, single-purpose primitives.
 */
public interface IdempotencyPort {
    /**
     * Atomically attempts to acquire an exclusive lock by creating a 'STARTED' record.
     * This is the equivalent of a SET-IF-NOT-EXISTS operation.
     * @param key The idempotency key.
     * @return true if the lock was acquired (key did not exist), false otherwise.
     */
    boolean tryAcquireLock(UUID key);

    /**
     * Retrieves the current data for an idempotency key.
     * This is called only after tryAcquireLock returns false.
     * @param key The idempotency key.
     * @return An Optional containing the data for the key.
     */
    Optional<IdempotencyData> getOperationData(UUID key);

    /**
     * Marks an operation as completed, stores its result, and persists the final state.
     * This operation effectively releases the lock.
     * @param key The key of the operation.
     * @param response The successful response object to save.
     */
    void markCompleted(UUID key, Object response);

    /**
     * Releases a lock without marking the operation as complete.
     * Used for cleanup after a processing failure.
     * @param key The key of the operation to release.
     */
    void releaseLock(UUID key);
}
