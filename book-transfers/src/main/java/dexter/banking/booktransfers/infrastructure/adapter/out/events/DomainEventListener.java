package dexter.banking.booktransfers.infrastructure.adapter.out.events;

import dexter.banking.booktransfers.core.domain.primitives.DomainEvent;

/**
 * A contract for a component that listens for a specific type of domain event.
 * This is a functional interface, making listener implementations clean and concise.
 * @param <T> The type of the DomainEvent this listener can handle.
 */
@FunctionalInterface
public interface DomainEventListener<T extends DomainEvent<?>> {
    void on(T event);
}
