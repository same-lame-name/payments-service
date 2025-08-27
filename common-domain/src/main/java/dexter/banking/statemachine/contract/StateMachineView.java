package dexter.banking.statemachine.contract;

import java.util.Set;

/**
 * A safe, read-only view of a StateMachine's state and configuration.
 */
public interface StateMachineView<S, E, C extends StateMachineContext<S>> {
    C getContext();
    S getCurrentState();
    S getInitialState();
    Set<S> getTerminalStates();
    boolean isComplete();
}
