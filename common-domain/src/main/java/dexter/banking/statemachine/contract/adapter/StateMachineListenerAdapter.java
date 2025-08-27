package dexter.banking.statemachine.contract.adapter;

import dexter.banking.statemachine.Transition;
import dexter.banking.statemachine.contract.StateMachineContext;
import dexter.banking.statemachine.contract.StateMachineListener;
import dexter.banking.statemachine.contract.StateMachineView;

/**
 * An abstract adapter class for {@link StateMachineListener}.
 * The methods in this class are empty. This class exists as a convenience for creating
 * listener objects; create a subclass and override only the methods of interest.
 *
 * @param <S> The type representing the state.
 * @param <E> The type representing the event.
 * @param <C> The type of the context object.
 */
public abstract class StateMachineListenerAdapter<S, E, C extends StateMachineContext<S>> implements StateMachineListener<S, E, C> {
    @Override
    public void stateChanged(S from, S to, StateMachineView<S, E, C> machine) {}

    @Override
    public void stateEntered(S state, StateMachineView<S, E, C> machine) {}

    @Override
    public void stateExited(S state, StateMachineView<S, E, C> machine) {}

    @Override
    public void eventNotAccepted(E event, S currentState, StateMachineView<S, E, C> machine) {}

    @Override
    public void transitionStarted(Transition<S, E, C> transition, StateMachineView<S, E, C> machine) {}

    @Override
    public void transitionEnded(Transition<S, E, C> transition, StateMachineView<S, E, C> machine) {}

    @Override
    public void onTransitionError(Transition<S, E, C> transition, StateMachineView<S, E, C> machine, Exception ex) {}
}
