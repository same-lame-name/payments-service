package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.payment;

import dexter.banking.booktransfers.core.port.out.OrchestrationContextRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * A dedicated, technical persistence adapter for the state machine context.
 * It implements the OrchestrationContextRepositoryPort and has only one job:
 * to save and retrieve the raw byte array of the serialized context.
 */
@Component
@RequiredArgsConstructor
class MongoOrchestrationContextRepository implements OrchestrationContextRepositoryPort {

    private final SpringMongoTransactionRepository repository;

    @Override
    public void save(UUID transactionId, byte[] contextData) {
        TransactionDocument doc = repository.findByTransactionId(transactionId)
                .orElseThrow(() -> new IllegalStateException("Cannot save context for a non-existent transaction: " + transactionId));
        doc.setOrchestrationContext(contextData);
        repository.save(doc);
    }

    @Override
    public Optional<byte[]> findById(UUID transactionId) {
        return repository.findByTransactionId(transactionId)
                .map(TransactionDocument::getOrchestrationContext);
    }
}
