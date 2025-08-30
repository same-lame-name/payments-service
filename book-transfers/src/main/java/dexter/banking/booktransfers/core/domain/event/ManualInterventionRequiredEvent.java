package dexter.banking.booktransfers.core.domain.event;

import dexter.banking.booktransfers.core.domain.model.PaymentState;
import dexter.banking.booktransfers.core.domain.primitives.DomainEvent;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ManualInterventionRequiredEvent(
    UUID eventId,
    UUID aggregateId,
    Instant occurredOn,
    PaymentState aggregateState,
    String reason,
    Map<String, Object> metadata
) implements DomainEvent<UUID> {

    public ManualInterventionRequiredEvent(UUID aggregateId, PaymentState aggregateState, String reason, Map<String, Object> metadata) {
        this(UUID.randomUUID(), aggregateId, Instant.now(), aggregateState, reason, metadata);
    }
}
