package dexter.banking.statemachine.contract;


import dexter.banking.statemachine.Transition;

/**
 * An interceptor interface that provides hooks to execute logic before and after a transition.
 * This is useful for cross-cutting concerns like auditing, security checks, or metrics.
 *
 * @param <S> The type representing the state.
 * @param <E> The type representing the event.
 * @param <C> The type of the context object.
 */
public interface StateMachineInterceptor<S, E, C extends StateMachineContext<S>> {
    /**
     * Called immediately before the transition's guard and action are executed.
     *
     * @param transition The transition that is about to occur.
     * @param machine The view only statemachine object.
     */
    void preTransition(Transition<S, E, C> transition, StateMachineView<S, E, C> machine);

    /**
     * Called after the state has been changed in the context and persisted (if a persister is configured).
     *
     * @param transition The transition that has just completed.
     * @param machine The view only statemachine object.
     */
    void postTransition(Transition<S, E, C> transition, StateMachineView<S, E, C> machine);

}
