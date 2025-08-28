package dexter.banking.booktransfers.core.domain.event;

import dexter.banking.booktransfers.core.domain.primitives.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentFailedEvent(
    UUID eventId,
    UUID aggregateId,
    Instant occurredOn,
    String reason,
    Map<String, Object> metadata
) implements DomainEvent<UUID> {

    public PaymentFailedEvent(UUID aggregateId, String reason, Map<String, Object> metadata) {
        this(UUID.randomUUID(), aggregateId, Instant.now(), reason, metadata);
    }
}
