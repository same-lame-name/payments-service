package dexter.banking.statemachine.contract;
import java.util.Optional;

/**
 * An interface for persisting and retrieving the state machine's context.
 * This abstraction allows the state machine to be durable and recover from failures.
 *
 * @param <S> The type representing the state.
 * @param <C> The type of the context object.
 */
public interface StateMachinePersister<S, C extends StateMachineContext<S>> {
    /**
     * Saves the current state of the context object to a durable store.
     *
     * @param context The context object to save.
     */
    void saveContext(C context);

    /**
     * Finds and retrieves a context object by its unique ID.
     * This is the crucial method for resuming a state machine after a shutdown or crash.
     *
     * @param id The unique identifier of the context.
     * @return An Optional containing the context if found, otherwise an empty Optional.
     */
    Optional<C> findContextById(String id);
}
