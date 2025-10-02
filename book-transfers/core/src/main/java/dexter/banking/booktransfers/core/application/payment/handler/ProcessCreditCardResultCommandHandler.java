package dexter.banking.booktransfers.core.application.payment.handler;

import dexter.banking.booktransfers.core.application.payment.command.callback.ProcessCreditCardResultCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.CreditLegResult;
import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.OrchestratedPaymentBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.BlueprintAccessor;
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
public class ProcessCreditCardResultCommandHandler implements CommandHandler<ProcessCreditCardResultCommand, Void> {

    private final PaymentRepositoryPort paymentRepository;
    private final BusinessPolicyFactory policyFactory;
    private final EventDispatcherPort eventDispatcher;
    private final BlueprintAccessor blueprintAccessor;

    private final StateMachineFactory<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> v2StateMachineFactory;

    @Override
    @Transactional
    public Void handle(ProcessCreditCardResultCommand command) {
        log.info("Handling credit card callback for transactionId: {}", command.transactionId());

        Payment.PaymentMemento memento = paymentRepository.findMementoById(command.transactionId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + command.transactionId()));

        Payment payment = rehydratePayment(memento);

        // Universal Routing Logic
        String journeyName = memento.journeyName();
        if (journeyName.contains("V2_ASYNC")) {
            resumeV2Orchestration(command, payment);
        } else {
            log.error("Unknown journeyName '{}' for callback on transactionId {}", journeyName, command.transactionId());
        }

        return null;
    }

    private void resumeV2Orchestration(ProcessCreditCardResultCommand command, Payment payment) {
        v2StateMachineFactory.acquireStateMachine(payment.getId().toString()).ifPresentOrElse(
                stateMachine -> {
                    var context = stateMachine.getContext();
                    Map<String, Object> metadata = new HashMap<>();
                    metadata.put("webhookUrl", context.getWebhookUrl());
                    metadata.put("realtime", context.getRealtime());
                    metadata.put("transactionReference", payment.getTransactionReference());
                    recordAndPublish(command, payment, metadata);
                    AsyncProcessEvent event = command.result().status() == CreditLegResult.CreditLegStatus.SUCCESSFUL ?
                            AsyncProcessEvent.CREDIT_LEG_SUCCEEDED : AsyncProcessEvent.CREDIT_LEG_FAILED;
                    stateMachine.fire(event);
                },
                () -> log.error("Could not acquire V2 state machine for transaction id: {}", command.transactionId())
        );
    }


    private Payment rehydratePayment(Payment.PaymentMemento memento) {
        OrchestratedPaymentBlueprint blueprint = blueprintAccessor.get(OrchestratedPaymentBlueprint.class);
        BusinessPolicy policy = policyFactory.create(blueprint.getPolicies());
        return Payment.rehydrate(memento, policy);
    }

    private void recordAndPublish(ProcessCreditCardResultCommand command, Payment payment, Map<String, Object> metadata) {
        payment.recordCredit(command.result(), metadata);

        paymentRepository.update(payment);
        eventDispatcher.dispatch(payment.pullDomainEvents());
    }
}
