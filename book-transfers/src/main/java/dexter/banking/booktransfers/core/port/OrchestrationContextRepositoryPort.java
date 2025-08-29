package dexter.banking.booktransfers.core.port;

import java.util.Optional;
import java.util.UUID;

/**
 * A dedicated, lean Driven Port for persisting the state machine's technical context.
 */
public interface OrchestrationContextRepositoryPort {
    /**
     * Persists the serialized state machine context.
     *
     * @param transactionId The unique ID of the transaction.
     * @param contextData   The raw byte array of the serialized context.
     */
    void save(UUID transactionId, byte[] contextData);

    /**
     * Retrieves the serialized state machine context.
     *
     * @param transactionId The unique ID of the transaction.
     * @return An Optional containing the raw byte array of the context if found.
     */
    Optional<byte[]> findById(UUID transactionId);
}
