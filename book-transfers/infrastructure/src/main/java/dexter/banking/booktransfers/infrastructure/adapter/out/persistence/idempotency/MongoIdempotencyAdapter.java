package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dexter.banking.booktransfers.core.domain.shared.idempotency.IdempotencyData;
import dexter.banking.booktransfers.core.domain.shared.idempotency.IdempotencyStatus;
import dexter.banking.booktransfers.core.port.out.IdempotencyPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoIdempotencyAdapter implements IdempotencyPort {

    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean tryAcquireLock(UUID key) {
        MongoIdempotencyRecord newRecord = new MongoIdempotencyRecord();
        newRecord.setIdempotencyKey(key.toString());
        newRecord.setStatus(MongoIdempotencyStatus.STARTED);
        newRecord.setCreatedAt(Instant.now());

        try {
            // This atomic operation is the core of the lock acquisition.
            mongoTemplate.insert(newRecord);
            return true; // Success, we created the record.
        } catch (DuplicateKeyException e) {
            // The key already exists, so we did not acquire the lock.
            log.debug("Idempotency key {} already exists, lock not acquired.", key);
            return false;
        }
    }

    @Override
    public Optional<IdempotencyData> getOperationData(UUID key) {
        Query query = new Query(Criteria.where("_id").is(key.toString()));
        MongoIdempotencyRecord record = mongoTemplate.findOne(query, MongoIdempotencyRecord.class);
        return Optional.ofNullable(record).map(this::toDomain);
    }

    @Override
    public void markCompleted(UUID key, Object response) {
        try {
            String responseBody = (response == null || response.getClass().equals(Void.class) || response.getClass().equals(Void.TYPE)) ?
                    null : objectMapper.writeValueAsString(response);
            String responseClassName = (response == null) ? Void.class.getName() : response.getClass().getName();

            Query query = new Query(Criteria.where("_id").is(key.toString()));
            Update update = new Update()
                    .set("status", MongoIdempotencyStatus.COMPLETED)
                    .set("responseBody", responseBody)
                    .set("responseClassName", responseClassName);
            mongoTemplate.updateFirst(query, update, MongoIdempotencyRecord.class);
        } catch (JsonProcessingException e) {
            log.error("FATAL: Failed to serialize response for idempotency key {}. Lock will be released.", key, e);
            releaseLock(key); // Ensure we don't leave a dangling lock
            throw new RuntimeException("Failed to serialize idempotent response", e);
        }
    }

    @Override
    public void releaseLock(UUID key) {
        log.warn("Releasing idempotency lock for key: {}", key);
        Query query = new Query(Criteria.where("_id").is(key.toString()));
        mongoTemplate.remove(query, MongoIdempotencyRecord.class);
    }

    // --- Anti-Corruption Mapping ---
    private IdempotencyData toDomain(MongoIdempotencyRecord document) {
        if (document == null) {
            return null;
        }
        Object response = document.getResponse(objectMapper)
                .orElse(null); // The raw response object

        IdempotencyStatus domainStatus =
                IdempotencyStatus.valueOf(document.getStatus().name());

        return new IdempotencyData(
                UUID.fromString(document.getIdempotencyKey()),
                domainStatus,
                response
        );
    }
}
