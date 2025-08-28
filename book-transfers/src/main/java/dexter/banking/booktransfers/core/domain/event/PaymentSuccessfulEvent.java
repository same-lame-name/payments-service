package dexter.banking.booktransfers.core.domain.event;

import dexter.banking.booktransfers.core.domain.primitives.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentSuccessfulEvent(
    UUID eventId,
    UUID aggregateId,
    Instant occurredOn,
    Map<String, Object> metadata
) implements DomainEvent<UUID> {

    public PaymentSuccessfulEvent(UUID aggregateId, Map<String, Object> metadata) {
        this(UUID.randomUUID(), aggregateId, Instant.now(), metadata);
    }
}
