package dexter.banking.booktransfers.infrastructure.adapter.out.cache.idempotency.redis;

import dexter.banking.booktransfers.core.domain.shared.idempotency.IdempotencyData;
import dexter.banking.booktransfers.core.domain.shared.idempotency.IdempotencyStatus;
import dexter.banking.booktransfers.core.port.out.IdempotencyPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
@Profile("idempotency-redis")
@RequiredArgsConstructor
class RedisIdempotencyAdapter implements IdempotencyPort {

    private final RedisTemplate<String, IdempotencyData> idempotencyRedisTemplate;
    private static final Duration EXPIRATION = Duration.ofHours(12);

    @Override
    public boolean tryAcquireLock(UUID key) {
        IdempotencyData startedData = new IdempotencyData(key, IdempotencyStatus.STARTED, null);
        // Atomically set the key if it does not exist, with a 12-hour TTL.
        return Boolean.TRUE.equals(
                idempotencyRedisTemplate.opsForValue().setIfAbsent(key.toString(), startedData, EXPIRATION)
        );
    }

    @Override
    public Optional<IdempotencyData> getOperationData(UUID key) {
        return Optional.ofNullable(idempotencyRedisTemplate.opsForValue().get(key.toString()));
    }

    @Override
    public void markCompleted(UUID key, Object response) {
        IdempotencyData completedData = new IdempotencyData(key, IdempotencyStatus.COMPLETED, response);
        // Overwrite the existing key with the final result, maintaining the TTL.
        idempotencyRedisTemplate.opsForValue().set(key.toString(), completedData, EXPIRATION);
    }

    @Override
    public void releaseLock(UUID key) {
        idempotencyRedisTemplate.delete(key.toString());
    }
}
