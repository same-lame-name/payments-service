package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.port.StateMachineContextRepositoryPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.contract.StateMachinePersister;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionStateMachinePersister implements StateMachinePersister<TransactionState, TransactionContext> {

    // The persister now depends on two clean, segregated ports.
    private final PaymentRepositoryPort paymentRepositoryPort;
    private final StateMachineContextRepositoryPort contextRepository;
    private final ObjectMapper objectMapper;
    @Override
    public void saveContext(TransactionContext context) {
        // 1. Update the business aggregate using the business repository port.
        paymentRepositoryPort.update(context.getPayment());

        // 2. Serialize the technical context and save it using the technical repository port.
        try {
            byte[] contextBytes = objectMapper.writeValueAsBytes(context);
            contextRepository.save(UUID.fromString(context.getId()), contextBytes);
            log.debug("Successfully persisted context for transactionId: {}", context.getId());
        } catch (JsonProcessingException e) {
            log.error("FATAL: Failed to serialize context for transactionId: {}", context.getId(), e);
            throw new RuntimeException("Failed to serialize context", e);
        }
    }

    @Override
    public Optional<TransactionContext> findContextById(String id) {
        // 1. Find the raw context data using the technical repository port.
        return contextRepository.findById(UUID.fromString(id))
            .flatMap(contextBytes -> {
                // 2. If found, deserialize it back into a full TransactionContext object.
                try {
                    log.debug("Found context data for transactionId: {}. Deserializing...", id);
                    return Optional.of(objectMapper.readValue(contextBytes, TransactionContext.class));
                } catch (IOException e) {
                    log.error("FATAL: Failed to deserialize context for transactionId: {}", id, e);
                    // Return empty if deserialization fails, preventing a corrupt state from spreading.
                    return Optional.empty();
                }
            });
    }
}
