package dexter.banking.booktransfers.infrastructure.adapter.out.events;

import dexter.banking.booktransfers.core.domain.primitives.DomainEvent;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * A concrete Driven Adapter for the EventDispatcherPort that uses Spring's
 * internal ApplicationEventPublisher. This allows us to leverage Spring's
 * powerful @TransactionalEventListener for robust, transaction-aware event handling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SpringEventDispatcherAdapter implements EventDispatcherPort {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void dispatch(List<DomainEvent<?>> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        log.info("Publishing {} domain event(s) to the application context...", events.size());
        events.forEach(eventPublisher::publishEvent);
    }
}
