package dexter.banking.statemachine.contract;

import dexter.banking.statemachine.Transition;

/**
 * A listener interface for receiving notifications about state machine events.
 * Implement this interface to hook into the lifecycle of state transitions for logging,
 * metrics, or other side-effects.
 *
 * @param <S> The type representing the state.
 * @param <E> The type representing the event.
 * @param <C> The type of the context object.
 */
public interface StateMachineListener<S, E, C extends StateMachineContext<S>> {
    void stateChanged(S from, S to, StateMachineView<S, E, C> machine);
    void stateEntered(S state, StateMachineView<S, E, C> machine);
    void stateExited(S state, StateMachineView<S, E, C> machine);
    void eventNotAccepted(E event, S currentState, StateMachineView<S, E, C> machine);
    void transitionStarted(Transition<S, E, C> transition, StateMachineView<S, E, C> machine);
    void transitionEnded(Transition<S, E, C> transition, StateMachineView<S, E, C> machine);

    /**
     * Called when an exception is thrown during the execution of a transition.
     * This includes exceptions from guards, actions, interceptors, or listeners.
     *
     * @param transition The transition that was being attempted.
     * @param machine The view only statemachine object.
     * @param ex         The exception that was caught.
     */
    void onTransitionError(Transition<S, E, C> transition, StateMachineView<S, E, C> machine, Exception ex);
}
