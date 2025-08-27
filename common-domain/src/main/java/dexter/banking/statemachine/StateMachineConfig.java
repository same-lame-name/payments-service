package dexter.banking.statemachine;

import dexter.banking.statemachine.contract.StateMachineInterceptor;
import dexter.banking.statemachine.contract.StateMachineListener;
import dexter.banking.statemachine.contract.StateMachinePersister;
import dexter.banking.statemachine.contract.StateMachineContext;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * An immutable configuration for a state machine.
 * <p>
 * This class holds the complete definition of a state machine's behavior, including
 * all transitions, listeners, interceptors, and terminal states. It is typically
 * constructed using the {@link StateMachineBuilder}.
 *
 * @param <S> The type representing the state.
 * @param <E> The type representing the event.
 * @param <C> The type of the context object.
 */
public class StateMachineConfig<S, E, C extends StateMachineContext<S>> {


    /**
     * An immutable key representing the unique combination of a source state and an event.
     * Used for efficiently looking up transitions.
     *
     * @param <S> The type representing the state.
     * @param <E> The type representing the event.
     */
    @Value
    public static class TransitionKey<S, E> {
        S source;
        E event;
    }

    private final Map<TransitionKey<S, E>, Transition<S, E, C>> transitions;
    private final S initialState;
    private final Set<S> terminalStates;
    private final StateMachinePersister<S, C> persister;
    private final List<StateMachineListener<S, E, C>> listeners;
    private final List<StateMachineInterceptor<S, E, C>> interceptors;

    public StateMachineConfig(Map<TransitionKey<S, E>, Transition<S, E, C>> transitions,
                              S initialState, Set<S> terminalStates,
                              StateMachinePersister<S, C> persister, List<StateMachineListener<S, E, C>> listeners, List<StateMachineInterceptor<S, E, C>> interceptors) {
        this.transitions = Map.copyOf(transitions);
        this.initialState = initialState;
        this.terminalStates = Set.copyOf(terminalStates);
        this.persister = persister;
        this.listeners = listeners;
        this.interceptors = interceptors;
    }

    // --- GETTERS ---
    public Transition<S, E, C> findTransition(S source, E event) {
        return transitions.get(new TransitionKey<>(source, event));
    }

    public Optional<StateMachinePersister<S, C>> getPersister() {
        return Optional.ofNullable(persister);
    }

    public S getInitialState() {
        return initialState;
    }

    public Set<S> getTerminalStates() {
        return terminalStates;
    }

    public List<StateMachineListener<S, E, C>> getListeners() {
        return listeners;
    }

    public List<StateMachineInterceptor<S, E, C>> getInterceptors() {
        return interceptors;
    }
}
