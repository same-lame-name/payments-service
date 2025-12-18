package dexter.banking.limit.pipeline.payee.middleware;

import com.fasterxml.jackson.databind.ObjectMapper;
import dexter.banking.limit.config.idempotency.TypeRegistry;
import dexter.banking.limit.config.model.ServiceConfig;
import dexter.banking.limit.domain.idempotency.IdempotencyRecord;
import dexter.banking.limit.domain.idempotency.IdempotencyStatus;
import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.service.idempotency.IdempotencyService;
import dexter.banking.limit.web.dto.PayeeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdempotencyMiddleware implements PipelineMiddleware<PayeeDto> {

    private final IdempotencyService idempotencyService;
    private final TypeRegistry typeRegistry;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public <R> R process(PayeeDto request, Next<R> next) {
        String key = request.getIdempotencyKey();
        ServiceConfig serviceConfig = request.getServiceConfig();
        if (!serviceConfig.idempotencyEnabled()|| key == null || key.isBlank()) {
            return next.invoke(); // Skip if no key
        }

        if (idempotencyService.tryAcquireLock(key)) {
            try {
                R response = next.invoke();
                
                String responseTypeAlias = typeRegistry.getAlias(response.getClass());
                String jsonResponse = objectMapper.writeValueAsString(response);
                
                idempotencyService.markCompleted(key, jsonResponse, responseTypeAlias);
                
                return response;
            } catch (Exception e) {
                idempotencyService.releaseLock(key);
                throw new RuntimeException(e);
//                throw e;
            }
        } else {
            IdempotencyRecord record = idempotencyService.getOperationData(key)
                    .orElseThrow(() -> new IllegalStateException("FATAL: Lock failed but no record found for key: " + key));

            if (record.getStatus() == IdempotencyStatus.COMPLETED) {
                try {
                    Class<?> responseClass = typeRegistry.getClass(record.getResponseTypeAlias());
                    Object cachedResponse = objectMapper.readValue(record.getResponsePayload(), responseClass);
                    return (R) cachedResponse;
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to deserialize cached response for key: " + key, e);
                }
            } else {
                throw new IllegalStateException("Request with key " + key + " is already in progress."); // This should be a custom 409 exception
            }
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }
}