package dexter.banking.booktransfers.core.usecase.payment.orchestration.async;

import dexter.banking.booktransfers.core.domain.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.domain.model.config.CommandConfiguration;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.ConfigurationPort;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.PaymentPolicyFactory;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.component.OrchestrationContextMapper;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessState;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.statemachine.StateMachine;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.UUID;
import java.util.function.BiConsumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class AsyncPaymentV2CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    private final StateMachineFactory<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> stateMachineFactory;
    private final PaymentRepositoryPort paymentRepository;
    private final EventDispatcherPort eventDispatcher;
    private final PaymentPolicyFactory policyFactory;
    private final ConfigurationPort configurationPort;
    private final OrchestrationContextMapper orchestrationContextMapper;

    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V2 && command.getModeOfTransfer() == ModeOfTransfer.ASYNC;
    }

    @Override
    @Transactional
    public PaymentResult handle(PaymentCommand command) {
        String journeyName = configurationPort.findForCommand(command.getIdentifier())
                .map(CommandConfiguration::journeyName)
                .orElseThrow(() -> new IllegalStateException("No journey configured for command: " + command.getIdentifier()));

        BusinessPolicy policy = policyFactory.getPolicyForJourney(journeyName);
        Payment payment = Payment.startNew(command, policy, journeyName);
        paymentRepository.save(payment);

        var context = orchestrationContextMapper.toContext(payment.getId(), command);
        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(AsyncProcessEvent.SUBMIT);

        return PaymentResult.from(payment);
    }

    @Transactional
    public void processCreditLegResult(CreditLegResult result, UUID transactionId) {
        handleAsyncEvent(transactionId, (payment, sm) -> {
            payment.recordCredit(result, Collections.emptyMap());
            AsyncProcessEvent event = result.status() == CreditLegResult.CreditLegStatus.SUCCESSFUL ?
                    AsyncProcessEvent.CREDIT_LEG_SUCCEEDED : AsyncProcessEvent.CREDIT_LEG_FAILED;
            sm.fire(event);

        });
    }

    @Transactional
    public void processDebitLegResult(DebitLegResult result, UUID transactionId) {
        handleAsyncEvent(transactionId, (payment, sm) -> {
            payment.recordDebit(result, Collections.emptyMap());
            AsyncProcessEvent event = switch (result.status()) {
                case SUCCESSFUL -> AsyncProcessEvent.DEBIT_LEG_SUCCEEDED;
                case FAILED -> AsyncProcessEvent.DEBIT_LEG_FAILED;
                default -> throw new IllegalStateException("Unexpected status for debit leg result: " + result.status());
            };
            sm.fire(event);
        });
    }

    @Transactional
    public void processLimitEarmarkResult(LimitEarmarkResult result, UUID transactionId) {
        handleAsyncEvent(transactionId, (payment, sm) -> {
            payment.recordLimitEarmark(result, Collections.emptyMap());
            AsyncProcessEvent event = switch (result.status()) {
                case SUCCESSFUL -> AsyncProcessEvent.LIMIT_EARMARK_SUCCEEDED;
                case FAILED -> AsyncProcessEvent.LIMIT_EARMARK_FAILED;

                default -> throw new IllegalStateException("Unexpected status for limit earmark result: " + result.status());
            };
            sm.fire(event);
        });
    }

    @Transactional
    public void processDebitReversalResult(DebitLegResult result, UUID transactionId) {
        handleAsyncEvent(transactionId, (payment, sm) -> {
            payment.recordDebitReversal(result, Collections.emptyMap());
            AsyncProcessEvent event = switch (result.status()) {
                case REVERSAL_SUCCESSFUL -> AsyncProcessEvent.DEBIT_LEG_REVERSAL_SUCCEEDED;
                case REVERSAL_FAILED -> AsyncProcessEvent.DEBIT_LEG_REVERSAL_FAILED;

                default -> throw new IllegalStateException("Unexpected status for debit reversal result: " + result.status());
            };
            sm.fire(event);
        });
    }

    @Transactional
    public void processLimitReversalResult(LimitEarmarkResult result, UUID transactionId) {
        handleAsyncEvent(transactionId, (payment, sm) -> {
            payment.recordLimitReversal(result, Collections.emptyMap());
            AsyncProcessEvent event = switch (result.status()) {
                case REVERSAL_SUCCESSFUL -> AsyncProcessEvent.LIMIT_EARMARK_REVERSAL_SUCCEEDED;
                case REVERSAL_FAILED -> AsyncProcessEvent.LIMIT_EARMARK_REVERSAL_FAILED;

                default -> throw new IllegalStateException("Unexpected status for limit reversal result: " + result.status());
            };
            sm.fire(event);
        });
    }

    private void handleAsyncEvent(UUID transactionId, BiConsumer<Payment, StateMachine<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext>> handler) {
        // 1. Rehydrate Aggregate
        Payment.PaymentMemento memento = paymentRepository.findMementoById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + transactionId));
        BusinessPolicy policy = policyFactory.getPolicyForJourney(memento.journeyName());
        Payment payment = Payment.rehydrate(memento, policy);

        // 2. Acquire State Machine
        stateMachineFactory.acquireStateMachine(transactionId.toString()).ifPresentOrElse(
                stateMachine -> {
                    // 3. Execute logic
                    handler.accept(payment, stateMachine);


                    // 4. Commit Unit of Work
                    paymentRepository.update(payment);
                    eventDispatcher.dispatch(payment.pullDomainEvents());
                },
                () -> log.error("Could not acquire state machine for transaction id: {}", transactionId)
        );
    }
}
