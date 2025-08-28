package dexter.banking.booktransfers.core.domain.primitives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AggregateRoot<ID> extends Entity<ID> {

    private final transient List<DomainEvent<?>> domainEvents = new ArrayList<>();

    protected AggregateRoot(ID id) {
        super(id);
    }

    /**
     * Registers a domain event to be dispatched.
     * @param event The domain event to register.
     */
    public void registerEvent(DomainEvent<?> event) {
        this.domainEvents.add(event);
    }

    /**
     * Pulls all pending domain events for dispatch.
     * This is a "get-and-clear" operation, ensuring events are dispatched only once.
     * @return An unmodifiable list of the pending domain events.
     */
    public List<DomainEvent<?>> pullDomainEvents() {
        if (domainEvents.isEmpty()) {
            return Collections.emptyList();
        }
        List<DomainEvent<?>> events = List.copyOf(domainEvents);
        this.domainEvents.clear();
        return events;
    }
}
