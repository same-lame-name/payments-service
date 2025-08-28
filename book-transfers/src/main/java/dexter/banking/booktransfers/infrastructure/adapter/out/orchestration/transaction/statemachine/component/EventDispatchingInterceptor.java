package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.component;

import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.Transition;
import dexter.banking.statemachine.contract.StateMachineInterceptor;
import dexter.banking.statemachine.contract.StateMachineView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * A state machine interceptor that dispatches domain events after a transition is complete and persisted.
 * This ensures that side effects are only triggered after the primary state change is durable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EventDispatchingInterceptor implements StateMachineInterceptor<TransactionState, TransactionEvent, TransactionContext> {

    private final EventDispatcherPort eventDispatcher;

    @Override
    public void preTransition(Transition<TransactionState, TransactionEvent, TransactionContext> transition, StateMachineView<TransactionState, TransactionEvent, TransactionContext> machine) {
        // Do nothing before the transition.
    }

    @Override
    public void postTransition(Transition<TransactionState, TransactionEvent, TransactionContext> transition, StateMachineView<TransactionState, TransactionEvent, TransactionContext> machine) {
        var payment = machine.getContext().getPayment();
        var targetState = transition.getTarget();

        log.info("The explicit value set in the action leg is {} and the transition is leading us to state {}", payment.getState(), targetState);
        log.info("EventDispatchingInterceptor: Dispatching events after transition to state {}", machine.getCurrentState());

        var events = payment.pullDomainEvents();
        eventDispatcher.dispatch(events);
    }
}
