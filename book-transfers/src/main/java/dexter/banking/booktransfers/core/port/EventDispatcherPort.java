package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.primitives.DomainEvent;

import java.util.List;

/**
 * A Driving Port for dispatching domain events to interested listeners.
 */
public interface EventDispatcherPort {
    void dispatch(List<DomainEvent<?>> events);
}
