package dexter.banking.booktransfers.core.port;

import java.util.Optional;
import java.util.UUID;

/**
 * A dedicated, fine-grained Driven Port for the state machine's technical persistence needs.
 * This adheres to the Interface Segregation Principle. Its only job is to save and retrieve
 * the raw, serialized context of the state machine, decoupling the orchestration adapter from
 * the specifics of the persistence technology.
 */
public interface StateMachineContextRepository {
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
