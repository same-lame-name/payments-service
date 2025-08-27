package dexter.banking.statemachine.contract;
/**
 * A contract for a guard condition that can veto a state machine transition.
 * <p>
 * A Guard serves as a final pre-condition check. If it evaluates to false,
 * the transition is halted, and its action is not executed.
 * <p>
 * This is a functional interface, allowing it to be implemented by a simple lambda
 * or a dedicated class for more complex conditions requiring dependencies.
 *
 * @param <C> The Context object type
 */
@FunctionalInterface
public interface Guard<C, E> {

    /**
     * Evaluates the guard condition against the current context and the event being fired.
     *
     * @param context The state machine context at the moment of transition.
     * @param event The event that triggered this transition.
     * @return {@code true} to allow the transition, {@code false} to veto it.
     */
    boolean evaluate(C context, E event);
}

