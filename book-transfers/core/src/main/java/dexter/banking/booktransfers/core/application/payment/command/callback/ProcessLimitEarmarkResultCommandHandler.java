package dexter.banking.booktransfers.core.application.payment.command.callback;

import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.LimitEarmarkResult;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.ConfigurationPort;
import dexter.banking.booktransfers.core.port.out.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProcessLimitEarmarkResultCommandHandler implements CommandHandler<ProcessLimitEarmarkResultCommand, Void> {

    private final PaymentRepositoryPort paymentRepository;
    private final ConfigurationPort configurationPort;
    private final BusinessPolicyFactory policyFactory;
    private final EventDispatcherPort eventDispatcher;

    @Qualifier("asyncTransactionFsmFactory")
    private final StateMachineFactory<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> v2StateMachineFactory;
    @Qualifier("v3TransactionFsmFactory")
    private final StateMachineFactory<ProcessStateV3, ProcessEventV3, HybridTransactionContext> v3StateMachineFactory;

    @Override
    @Transactional
    public Void handle(ProcessLimitEarmarkResultCommand command) {
        log.info("Handling limit earmark callback for transactionId: {}", command.transactionId());

        Payment.PaymentMemento memento = paymentRepository.findMementoById(command.transactionId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + command.transactionId()));

        Payment payment = rehydratePayment(memento);

        payment.recordLimitEarmark(command.result(), Collections.emptyMap());

        paymentRepository.update(payment);
        eventDispatcher.dispatch(payment.pullDomainEvents());

        // Universal Routing Logic - V3 does not have async limit earmark, so this only applies to V2.
        String journeyName = memento.journeyName();
        if (journeyName.contains("V2_ASYNC")) {
            resumeV2Orchestration(command, memento);
        } else {
            log.warn("Received a Limit Earmark callback for a non-V2-Async journey '{}'. Ignoring. TXN_ID: {}", journeyName, command.transactionId());
        }

        return null;
    }

    private void resumeV2Orchestration(ProcessLimitEarmarkResultCommand command, Payment.PaymentMemento memento) {
        v2StateMachineFactory.acquireStateMachine(memento.id().toString()).ifPresentOrElse(
                stateMachine -> {
                    AsyncProcessEvent event = command.result().status() == LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL ?
                            AsyncProcessEvent.LIMIT_EARMARK_SUCCEEDED : AsyncProcessEvent.LIMIT_EARMARK_FAILED;
                    stateMachine.fire(event);
                },
                () -> log.error("Could not acquire V2 state machine for transaction id: {}", command.transactionId())
        );
    }

    private Payment rehydratePayment(Payment.PaymentMemento memento) {
        BusinessPolicy policy = configurationPort
                .findForJourney(memento.journeyName())
                .map(policyFactory::create)
                .orElseThrow(() -> new IllegalStateException("No journey configured for identifier: " + memento.journeyName()));
        return Payment.rehydrate(memento, policy);
    }
}
