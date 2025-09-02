package dexter.banking.booktransfers.core.domain.compliance.event;

import dexter.banking.booktransfers.core.domain.shared.primitives.DomainEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public record ComplianceCaseApproved(
        UUID eventId,
        UUID aggregateId,
        Instant occurredOn,
        UUID paymentId
) implements DomainEvent<UUID> {
    public ComplianceCaseApproved(UUID aggregateId, UUID paymentId) {
        this(UUID.randomUUID(), aggregateId, Instant.now(), paymentId);
    }

    @Override
    public Map<String, Object> metadata() {
        return Collections.emptyMap();
    }
}
