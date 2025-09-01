package dexter.banking.booktransfers.core.domain.payment.event;


import dexter.banking.booktransfers.core.domain.payment.PaymentState;
import dexter.banking.booktransfers.core.domain.shared.primitives.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record PaymentSuccessfulEvent(
    UUID eventId,
    UUID aggregateId,
    Instant occurredOn,
    PaymentState aggregateState,
    Map<String, Object> metadata
) implements DomainEvent<UUID> {

    public PaymentSuccessfulEvent(UUID aggregateId, PaymentState aggregateState, Map<String, Object> metadata) {
        this(UUID.randomUUID(), aggregateId, Instant.now(), aggregateState, metadata);
    }
}
