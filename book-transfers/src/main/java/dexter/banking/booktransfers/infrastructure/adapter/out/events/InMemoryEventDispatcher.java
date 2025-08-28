package dexter.banking.booktransfers.infrastructure.adapter.out.events;

import dexter.banking.booktransfers.core.domain.primitives.DomainEvent;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class InMemoryEventDispatcher implements EventDispatcherPort {

    private final Map<Class<?>, List<DomainEventListener>> listeners;

    public InMemoryEventDispatcher(List<DomainEventListener<?>> allListeners) {
        this.listeners = new HashMap<>();
        for (DomainEventListener<?> listener : allListeners) {
            Class<?> eventType = getEventTypeForListener(listener.getClass());
            this.listeners.computeIfAbsent(eventType, k -> new ArrayList<>()).add(listener);
        }
        log.info("Initialized InMemoryEventDispatcher with {} listener types.", listeners.size());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void dispatch(List<DomainEvent<?>> events) {
        if (events == null || events.isEmpty()) {
            log.info("No domain events found!");
            return;
        }

        for (DomainEvent<?> event : events) {
            List<DomainEventListener> relevantListeners = listeners.getOrDefault(event.getClass(), Collections.emptyList());
            if (relevantListeners.isEmpty()) {
                log.info("No listeners found for event type: {}", event.getClass().getSimpleName());
                continue;
            }

            log.info("Dispatching event {} to {} listener(s)", event.getClass().getSimpleName(), relevantListeners.size());
            for (DomainEventListener listener : relevantListeners) {
                try {
                    listener.on(event);
                } catch (Exception e) {
                    // In a real system, failed listeners should be handled with a dead-letter queue or retry mechanism.
                    log.error("Listener {} failed to process event {}", listener.getClass().getSimpleName(), event, e);
                }
            }
        }
    }

    private Class<?> getEventTypeForListener(Class<?> listenerClass) {
        // Walk up the hierarchy to find the generic interface
        Type[] genericInterfaces = listenerClass.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType pt && pt.getRawType().equals(DomainEventListener.class)) {
                Type eventType = pt.getActualTypeArguments()[0];
                if (eventType instanceof Class) {
                    return (Class<?>) eventType;
                }
            }
        }
        throw new IllegalStateException("Could not determine event type for listener " + listenerClass.getName());
    }
}
