package dexter.banking.statemachine;

import dexter.banking.statemachine.contract.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The core state machine engine that drives transitions based on events.
 * <p>
 * This class is responsible for processing events, finding the appropriate transition,
 * evaluating guards, executing actions, and managing state changes. It orchestrates
 * interactions with the {@link StateMachineConfig}, {@link StateMachinePersister},
 * {@link StateMachineListener}, and {@link StateMachineInterceptor}.
 *
 * @param <S> The type representing the state, typically an enum.
 * @param <E> The type representing the event, typically an enum.
 * @param <C> The type of the context object that holds the state.
 */
public class StateMachine<S, E, C extends StateMachineContext<S>> implements StateMachineView<S, E, C> {

    private static final Logger log = LoggerFactory.getLogger(StateMachine.class);
    private final C context;
    private final StateMachineConfig<S, E, C> config;
    private final Optional<StateMachinePersister<S, C>> persister;
    private final List<StateMachineListener<S, E, C>> listeners;
    private final List<StateMachineInterceptor<S, E, C>> interceptors;
    private boolean isFiring = false; // The re-entrant guard flag

    /**
     * Constructs a new StateMachine instance.
     *
     * @param context   The context object (e.g., a Transaction) this state machine will manage. Must not be null.
     * @param config    The configuration defining the states, transitions, and behavior of the FSM. Must not be null.
     */
    public StateMachine(C context, StateMachineConfig<S, E, C> config) {
        this.context = Objects.requireNonNull(context, "Context cannot be null");
        this.config = Objects.requireNonNull(config, "Configuration cannot be null");
        this.persister = config.getPersister();
        this.listeners = config.getListeners();
        this.interceptors = config.getInterceptors();
    }

    /**
     * Fires an event, triggering a state transition.
     * <p>
     * If a transition's action returns another event, that event will be fired immediately
     * in a cascade until no more events are returned or a terminal state is reached.
     * If any part of the transition (guard, action, listener, interceptor) throws an exception,
     * the process is halted, and the {@link StateMachineListener#onTransitionError(Transition, StateMachineView, Exception)}
     * callback is invoked.
     *
     * @param initialEvent The first event to fire.
     */
    public void fire(E initialEvent) {
        if (isFiring) {
            throw new IllegalStateException("Re-entrant fire() call detected. Event [" + initialEvent + "] cannot be fired while the machine is already processing.");
        }

        this.isFiring = true;

        try {
            Optional<E> currentEventOpt = Optional.of(initialEvent);

            while (currentEventOpt.isPresent()) {
                E currentEvent = currentEventOpt.get();
                S currentState = context.getCurrentState();

                if (isComplete()) {
                    log.warn("Cannot fire event [{}]. State machine is in a terminal state [{}].", currentEvent, getCurrentState());
                    return;
                }

                Transition<S, E, C> transition = config.findTransition(currentState, currentEvent);

                if (transition == null) {
                    log.warn("No transition found for state [{}] on event [{}]", currentState, currentEvent);
                    listeners.forEach(l -> l.eventNotAccepted(currentEvent, currentState, this));
                    return; // Stop the cascade if an event is not accepted
                }

                try {
                    listeners.forEach(l -> l.transitionStarted(transition, this));
                    interceptors.forEach(i -> i.preTransition(transition, this));


                    if (!transition.getGuard().evaluate(context, currentEvent)) {
                        log.info("Guard prevented transition from [{}] on event [{}]", currentState, currentEvent);
                        listeners.forEach(l -> l.eventNotAccepted(currentEvent, currentState, this));
                        return; // Stop the cascade if a guard fails
                    }

                    log.info("Transitioning from [{}] to [{}] on event [{}]", currentState, transition.getTarget(), currentEvent);
                    listeners.forEach(l -> l.stateExited(currentState, this));

                    // Execute the action and capture the next event for a potential cascade
                    Optional<E> nextEvent = transition.getAction().execute(context, currentEvent);

                    context.setCurrentState(transition.getTarget());

                    listeners.forEach(l -> l.stateEntered(transition.getTarget(), this));
                    listeners.forEach(l -> l.stateChanged(currentState, transition.getTarget(), this));

                    persister.ifPresent(persister -> persister.saveContext(context));

                    interceptors.forEach(i -> i.postTransition(transition, this));
                    listeners.forEach(l -> l.transitionEnded(transition, this));

                    // Set up the next iteration of the loop for the cascade
                    currentEventOpt = nextEvent;

                } catch (Exception ex) {
                    log.error("Error during transition from [{}] on event [{}]. Halting state machine.", currentState, currentEvent, ex);
                    // Notify listener about the failure
                    listeners.forEach(l -> l.onTransitionError(transition, this, ex));
                    // Halt the cascade
                    return;
                }
            }
        } finally {
            this.isFiring = false;
        }
    }

    // --- StateMachineView Implementation ---
    @Override public C getContext() { return context; }
    @Override public S getCurrentState() { return context.getCurrentState(); }
    @Override public S getInitialState() { return config.getInitialState(); }
    @Override public Set<S> getTerminalStates() { return config.getTerminalStates(); }
    @Override public boolean isComplete() { return config.getTerminalStates().contains(context.getCurrentState()); }
}
