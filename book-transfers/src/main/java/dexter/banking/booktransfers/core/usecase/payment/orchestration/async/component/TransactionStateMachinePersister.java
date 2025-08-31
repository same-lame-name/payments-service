package dexter.banking.booktransfers.core.usecase.payment.orchestration.async.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dexter.banking.booktransfers.core.port.OrchestrationContextRepositoryPort;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessState;
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
public class TransactionStateMachinePersister implements StateMachinePersister<AsyncProcessState, AsyncTransactionContext> {

    private final OrchestrationContextRepositoryPort contextRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void saveContext(AsyncTransactionContext context) {
        try {
            byte[] contextBytes = objectMapper.writeValueAsBytes(context);
            contextRepository.save(context.getPaymentId(), contextBytes);
            log.debug("Successfully persisted context for transactionId: {}", context.getId());
        } catch (JsonProcessingException e) {
            log.error("FATAL: Failed to serialize context for transactionId: {}", context.getId(), e);
            throw new RuntimeException("Failed to serialize context", e);
        }
    }

    @Override
    public Optional<AsyncTransactionContext> findContextById(String id) {
        return contextRepository.findById(UUID.fromString(id))
            .flatMap(contextBytes -> {
                try {
                    log.debug("Found context data for transactionId: {}. Deserializing...", id);
                    return Optional.of(objectMapper.readValue(contextBytes, AsyncTransactionContext.class));
                } catch (IOException e) {
                    log.error("FATAL: Failed to deserialize context for transactionId: {}", id, e);
                    return Optional.empty();
                }
            });
    }
}
