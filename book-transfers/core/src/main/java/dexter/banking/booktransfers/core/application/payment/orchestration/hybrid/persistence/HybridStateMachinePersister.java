package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.port.out.OrchestrationContextRepositoryPort;
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
public class HybridStateMachinePersister implements StateMachinePersister<ProcessStateV3, HybridTransactionContext> {
    private final OrchestrationContextRepositoryPort contextRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void saveContext(HybridTransactionContext context) {
        try {
            byte[] contextBytes = objectMapper.writeValueAsBytes(context);
            contextRepository.save(context.getPaymentId(), contextBytes);
            log.debug("Successfully persisted hybrid context for transactionId: {}", context.getId());
        } catch (JsonProcessingException e) {
            log.error("FATAL: Failed to serialize hybrid context for transactionId: {}", context.getId(), e);
            throw new RuntimeException("Failed to serialize context", e);
        }
    }

    @Override
    public Optional<HybridTransactionContext> findContextById(String id) {
        return contextRepository.findById(UUID.fromString(id))
                .flatMap(contextBytes -> {
                    try {
                        log.debug("Found hybrid context data for transactionId: {}. Deserializing...", id);
                        return Optional.of(objectMapper.readValue(contextBytes, HybridTransactionContext.class));
                    } catch (IOException e) {
                        log.error("FATAL: Failed to deserialize hybrid context for transactionId: {}", id, e);
                        return Optional.empty();
                    }
                });
    }
}
