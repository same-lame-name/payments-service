package dexter.banking.statemachine;

import dexter.banking.statemachine.contract.Action;
import dexter.banking.statemachine.contract.Guard;
import dexter.banking.statemachine.contract.StateMachineContext;
import lombok.Value;

import java.util.Optional;

/**
 * Represents a single, immutable transition within the state machine.
 * A transition defines the path from a source state to a target state, triggered by an event.
 * It may include a guard condition and an action to be executed.
 *
 * @param <S> The type representing the state.
 * @param <E> The type representing the event.
 * @param <C> The type of the context object.
 */
@Value
public class Transition<S, E, C extends StateMachineContext<S>> {

    /** The state from which this transition originates. */
    S source;

    /** The event that triggers this transition. */
    E event;

    /** The state to which this transition leads. */
    S target;

    /** A predicate that must be true for the transition to proceed. */
    Guard<C, E> guard;

    /** A function to be executed as part of the transition, which can optionally return a new event to cascade. */
    Action<S, E, C> action;

    /**
     * A static factory method for creating a transition with a default (always true) guard and a no-op action if none are provided.
     *
     * @param source The source state.
     * @param event  The event.
     * @param target The target state.
     * @param guard  The guard condition (can be null).
     * @param action The action to execute (can be null).
     * @return A new, immutable Transition instance.
     */
    public static <S, E, C extends StateMachineContext<S>> Transition<S, E, C> of(
            S source, E event, S target, Guard<C, E> guard, Action<S, E, C> action) {

        // Provide default implementations for guard and action if they are null
        Guard<C, E> finalGuard = guard != null ? guard : (ctxt, evnt) -> true;
        Action<S, E, C> finalAction = action != null ? action : (ctxt, evnt) -> Optional.empty();

        return new Transition<>(source, event, target, finalGuard, finalAction);
    }
}