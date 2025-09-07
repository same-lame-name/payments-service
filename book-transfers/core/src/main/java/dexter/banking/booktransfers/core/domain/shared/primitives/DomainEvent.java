package dexter.banking.booktransfers.core.domain.shared.primitives;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * A contract for a significant event that occurred within the domain.
 * Domain events are immutable facts about the past.
 * @param <ID> The type of the ID of the aggregate that raised this event.
 */
public interface DomainEvent<ID> {
    /** @return A unique identifier for this specific event instance. */
    UUID eventId();

    /** @return The identifier of the aggregate that raised this event. */
    ID aggregateId();

    /** @return The exact time the event occurred. */
    Instant occurredOn();

    /** @return A flexible payload for application-level context (e.g., webhook URLs, correlation IDs). */
    Map<String, Object> metadata();
}
