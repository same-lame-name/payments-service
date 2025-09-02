package dexter.banking.booktransfers.core.domain.compliance.event;

import dexter.banking.booktransfers.core.domain.shared.primitives.DomainEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public record ComplianceCaseRejected(
        UUID eventId,
        UUID aggregateId,
        Instant occurredOn,
        UUID paymentId,
        String reason
) implements DomainEvent<UUID> {
    public ComplianceCaseRejected(UUID aggregateId, UUID paymentId, String reason) {
        this(UUID.randomUUID(), aggregateId, Instant.now(), paymentId, reason);
    }

    @Override
    public Map<String, Object> metadata() {
        return Collections.emptyMap();
    }
}
