package dexter.banking.statemachine.contract;

/**
 * An interface for the context object that the state machine manages.
 * The context is the entity that holds the current state and is passed to actions and guards.
 *
 * @param <S> The type representing the state.
 */
public interface StateMachineContext<S> {
    S getCurrentState();
    void setCurrentState(S newState);
    String getId(); // A unique identifier for persistence
}