package dexter.banking.statemachine.contract;
import java.util.Optional;

/**
 * A contract for an action to be performed during a state machine transition.
 * <p>
 * This is a functional interface, allowing it to be implemented by a lambda
 * or a dedicated class. Implementing it as a class is recommended for complex
 * actions that require their own dependencies.
 *
 * @param <S> The State enum type
 * @param <E> The Event enum type
 * @param <C> The Context object type
 */
@FunctionalInterface
public interface Action<S, E, C extends StateMachineContext<S>> {

    /**
     * Executes the business logic for this action.
     *
     * @param context The state machine context, providing access to the current state and other data.
     * @param event The event that triggered this transition.
     * @return An Optional containing the next event to be fired for a synchronous cascade,
     * or an empty Optional if the flow should pause and wait for an external event.
     */
    Optional<E> execute(C context, E event);
}
