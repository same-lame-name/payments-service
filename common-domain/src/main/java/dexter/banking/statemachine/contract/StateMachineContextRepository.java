package dexter.banking.statemachine.contract;

import java.util.Optional;

/**
 * A dedicated persistence port for the state machine's context.
 * <p>
 * This interface defines the contract for any adapter that wishes to provide
 * persistence for a state machine's context object. It is intentionally separate
 * from business-level repositories to adhere to the Interface Segregation Principle.
 *
 * @param <C> The type of the context object to be persisted.
 */
public interface StateMachineContextRepository<C> {

    /**
     * Saves the current state of the context object to a durable store.
     * <p>
     * <b>IMPORTANT:</b> Any implementation of this method MUST use an
     * optimistic locking strategy to prevent race conditions from concurrent
     * state machine events. It should throw a runtime exception (e.g., a custom
     * OptimisticLockingFailureException) if a concurrent modification is detected.
     *
     * @param context The context object to save.
     */
    void saveContext(C context);

    /**
     * Finds and retrieves a context object by its unique ID.
     * This is the crucial method for resuming a state machine's state.
     *
     * @param id The unique identifier of the context.
     * @return An Optional containing the context if found, otherwise an empty Optional.
     */
    Optional<C> findContextById(String id);
}
