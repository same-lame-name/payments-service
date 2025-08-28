package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.component;

import dexter.banking.booktransfers.core.domain.event.ManualInterventionRequiredEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentFailedEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentInProgressEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentSuccessfulEvent;
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

import java.util.HashMap;
import java.util.Map;

/**
 * A state machine interceptor that registers domain events after a transition is complete and persisted.
 * This ensures that side effects are only triggered after the primary state change is durable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterDomainEventInterceptor implements StateMachineInterceptor<TransactionState, TransactionEvent, TransactionContext> {

    @Override
    public void preTransition(Transition<TransactionState, TransactionEvent, TransactionContext> transition, StateMachineView<TransactionState, TransactionEvent, TransactionContext> machine) {
        // Do nothing before the transition.
    }

    @Override
    public void postTransition(Transition<TransactionState, TransactionEvent, TransactionContext> transition, StateMachineView<TransactionState, TransactionEvent, TransactionContext> machine) {
        var context = machine.getContext();
        var payment = context.getPayment();
        var command = context.getRequest();
        // The orchestrator's action is now responsible for building the metadata and registering the terminal domain event.
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("webhookUrl", command.getWebhookUrl());
        metadata.put("state", payment.getState());
        metadata.put("transactionReference", payment.getTransactionReference());

        switch (payment.getState()) {
            case TRANSACTION_SUCCESSFUL -> payment.registerEvent(new PaymentSuccessfulEvent(payment.getId(), metadata));
            case TRANSACTION_FAILED -> payment.registerEvent(new PaymentFailedEvent(payment.getId(), "Transaction flow ended in a failed state.", metadata));
            case MANUAL_INTERVENTION_REQUIRED -> payment.registerEvent(new ManualInterventionRequiredEvent(payment.getId(), "Transaction flow ended in a remediation state.", metadata));
            default -> {
                if ("true".equals(command.getRealtime())) {
                    payment.registerEvent(new PaymentInProgressEvent(payment.getId(), metadata));
                }
            }
        }

    }

}
