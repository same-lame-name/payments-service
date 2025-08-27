package dexter.banking.statemachine;

import dexter.banking.statemachine.contract.*;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * A fluent builder for creating an immutable {@link StateMachineConfig}.
 * This builder provides a readable DSL for defining states, transitions, actions, and guards.
 *
 * @param <S> The type representing the state.
 * @param <E> The type representing the event.
 * @param <C> The type of the context object.
 */
public class StateMachineBuilder<S, E, C extends StateMachineContext<S>> {
    private final Map<StateMachineConfig.TransitionKey<S, E>, Transition<S, E, C>> transitions = new HashMap<>();
    private final List<StateMachineListener<S, E, C>> listeners = new ArrayList<>();
    private final List<StateMachineInterceptor<S, E, C>> interceptors = new ArrayList<>();

    private S initialState;
    private final Set<S> terminalStates = new HashSet<>();
    private Set<S> allStates;
    private StateMachinePersister<S, C> persister;

    public static <S extends Enum<S>, E, C extends StateMachineContext<S>> StateMachineBuilder<S, E, C> newBuilder() {
        return new StateMachineBuilder<>();
    }

    public StateMachineBuilder<S, E, C> states(Set<S> stateSet) {
        this.allStates = stateSet;
        return this;
    }

    public StateMachineBuilder<S, E, C> initial(S initialState) {
        this.initialState = initialState;
        return this;
    }

    public StateMachineBuilder<S, E, C> end(S terminalState) {
        this.terminalStates.add(terminalState);
        return this;
    }

    public StateMachineBuilder<S, E, C> addListener(StateMachineListener<S, E, C> listener) {
        this.listeners.add(listener);
        return this;
    }

    public StateMachineBuilder<S, E, C> addInterceptor(StateMachineInterceptor<S, E, C> interceptor) {
        this.interceptors.add(interceptor);
        return this;
    }

    public StateMachineBuilder<S, E, C> withPersister(StateMachinePersister<S, C> persister) {
        this.persister = persister;
        return this;
    }

    public TransitionConfigurer from(S source) {
        return new TransitionConfigurer(source);
    }

    /**
     * Builds the immutable {@link StateMachineConfig} after validating the configuration.
     *
     * @return A new StateMachineConfig instance.
     * @throws IllegalStateException if the configuration is invalid (e.g., missing initial state).
     */
    public StateMachineConfig<S, E, C> build() {
        Objects.requireNonNull(allStates, "All possible states must be defined via .states()");
        Objects.requireNonNull(initialState, "An initial state must be defined via .initial()");

        if (!allStates.contains(initialState)) {
            throw new IllegalStateException("Initial state " + initialState + " is not in the set of defined states.");
        }
        for (S terminalState : terminalStates) {
            if (!allStates.contains(terminalState)) {
                throw new IllegalStateException("End state " + terminalState + " is not in the set of defined states.");
            }
        }
        for (Transition<S, E, C> transition : transitions.values()) {
            if (!allStates.contains(transition.getSource())) {
                throw new IllegalStateException("Transition source state " + transition.getSource() + " is not a registered state.");
            }
            if (!allStates.contains(transition.getTarget())) {
                throw new IllegalStateException("Transition target state " + transition.getTarget() + " is not a registered state.");
            }
            if (terminalStates.contains(transition.getSource())) {
                throw new IllegalStateException("Cannot define a transition from a terminal state: " + transition.getSource());
            }
        }
        return new StateMachineConfig<>(transitions, initialState, terminalStates, persister, List.copyOf(listeners), List.copyOf(interceptors));
    }

    /**
     * An inner fluent builder class for configuring a single transition.
     */
    public class TransitionConfigurer {
        private final S source;
        private E event;
        private S target;
        private Guard<C, E> guard;
        private Action<S, E, C> action;


        private TransitionConfigurer(S source) {
            this.source = source;
        }

        public TransitionConfigurer on(E event) {
            this.event = event;
            return this;
        }

        public TransitionConfigurer to(S target) {
            this.target = target;
            return this;
        }

        public TransitionConfigurer withGuard(Guard<C, E> guard) {
            this.guard = guard;
            return this;
        }

        public TransitionConfigurer withAction(Action<S, E, C> action) {
            // Adapt the interface to the underlying Function using a method reference.
            this.action = action;
            return this;
        }

        public TransitionConfigurer withAction(BiConsumer<C, E> action) {
            this.action = (context, event) -> {
                action.accept(context, event);
                return Optional.empty();
            };
            return this;
        }

        public StateMachineBuilder<S, E, C> add() {
            Objects.requireNonNull(source, "Source state cannot be null");
            Objects.requireNonNull(event, "Event cannot be null");
            Objects.requireNonNull(target, "Target state cannot be null");

            var key = new StateMachineConfig.TransitionKey<>(source, event);
            if (transitions.containsKey(key)) {
                throw new IllegalStateException("Duplicate transition defined for source " + source + " and event " + event);
            }

            transitions.put(key, Transition.of(source, event, target, guard, action));
            return StateMachineBuilder.this;
        }
    }
}
