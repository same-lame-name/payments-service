package dexter.banking.booktransfers.core.application.payment.handler;

import dexter.banking.booktransfers.core.application.payment.command.callback.ProcessLimitEarmarkResultCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.AsyncProcessState;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.payment.valueobject.LimitEarmarkResult;
import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.OrchestratedPaymentBlueprint;
import dexter.banking.booktransfers.core.domain.shared.markers.WithJourneyContext;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
class ProcessLimitEarmarkResultCommandHandler implements CommandHandler<ProcessLimitEarmarkResultCommand, Void> {

    private final PaymentRepositoryPort paymentRepository;
    private final BusinessPolicyFactory policyFactory;
    private final EventDispatcherPort eventDispatcher;

    private final StateMachineFactory<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> v2StateMachineFactory;

    @Override
    @Transactional
    public Void handle(ProcessLimitEarmarkResultCommand command) {
        log.info("Handling limit earmark callback for transactionId: {}", command.getTransactionId());

        Payment.PaymentMemento memento = paymentRepository.findMementoById(command.getTransactionId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + command.getTransactionId()));

        OrchestratedPaymentBlueprint blueprint = command.getBlueprint();
        BusinessPolicy policy = policyFactory.create(blueprint.getPolicies());
        Payment payment =  Payment.rehydrate(memento, policy);

        // Universal Routing Logic - V3 does not have async limit earmark, so this only applies to V2.
        String journeyName = memento.journeyName();
        if (journeyName.contains("V2_ASYNC")) {
            resumeV2Orchestration(command, payment);
        } else {
            log.warn("Received a Limit Earmark callback for a non-V2-Async journey '{}'. Ignoring. TXN_ID: {}", journeyName, command.getTransactionId());
        }

        return null;
    }

    @WithJourneyContext(journeyIdentifier = "#command.getIdentifier()")
    private void resumeV2Orchestration(ProcessLimitEarmarkResultCommand command, Payment payment) {
        v2StateMachineFactory.acquireStateMachine(payment.getId().toString()).ifPresentOrElse(
                stateMachine -> {
                    var context = stateMachine.getContext();
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("webhookUrl", context.getWebhookUrl());
                    metadata.put("realtime", context.getRealtime());
                    metadata.put("transactionReference", payment.getTransactionReference());
                    recordAndPublish(command, payment, metadata);
                    AsyncProcessEvent event = command.getResult().status() == LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL ?
                            AsyncProcessEvent.LIMIT_EARMARK_SUCCEEDED : AsyncProcessEvent.LIMIT_EARMARK_FAILED;
                    stateMachine.fire(event);
                },
                () -> log.error("Could not acquire V2 state machine for transaction id: {}", command.getTransactionId())
        );
    }

    private void recordAndPublish(ProcessLimitEarmarkResultCommand command, Payment payment, Map<String, Object> metadata) {
        payment.recordLimitEarmark(command.getResult(), metadata);

        paymentRepository.update(payment);
        eventDispatcher.dispatch(payment.pullDomainEvents());
    }
}
