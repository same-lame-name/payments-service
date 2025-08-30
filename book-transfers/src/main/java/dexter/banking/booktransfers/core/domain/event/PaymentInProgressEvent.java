package dexter.banking.booktransfers.core.domain.event;

import dexter.banking.booktransfers.core.domain.model.PaymentState;
import dexter.banking.booktransfers.core.domain.primitives.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentInProgressEvent(
    UUID eventId,
    UUID aggregateId,
    Instant occurredOn,
    PaymentState aggregateState,
    Map<String, Object> metadata
) implements DomainEvent<UUID> {

    public PaymentInProgressEvent(UUID aggregateId, PaymentState aggregateState, Map<String, Object> metadata) {
        this(UUID.randomUUID(), aggregateId, Instant.now(), aggregateState, metadata);
    }
}
