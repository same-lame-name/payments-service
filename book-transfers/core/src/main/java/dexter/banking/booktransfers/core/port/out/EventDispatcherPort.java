package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.domain.shared.primitives.DomainEvent;

import java.util.List;

/**
 * A Driving Port for dispatching domain events to interested listeners.
 */
public interface EventDispatcherPort {
    void dispatch(List<DomainEvent<?>> events);
}
