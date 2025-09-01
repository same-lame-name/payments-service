package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.booktransfers.core.application.payment.PaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.OrchestrationContextMapper;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.payment.result.CreditLegResult;
import dexter.banking.booktransfers.core.domain.payment.result.DebitLegResult;
import dexter.banking.booktransfers.core.domain.payment.result.LimitEarmarkResult;
import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContextHolder;
import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.ConfigurationPort;
import dexter.banking.booktransfers.core.port.out.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiFunction;

@Component
@RequiredArgsConstructor
@Slf4j
public class AsyncPaymentV2CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    private final StateMachineFactory<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> stateMachineFactory;
    private final PaymentRepositoryPort paymentRepository;
    private final EventDispatcherPort eventDispatcher;
    private final BusinessPolicyFactory policyFactory;
    private final ConfigurationPort configurationPort;
    private final OrchestrationContextMapper orchestrationContextMapper;

    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V2 && command.getModeOfTransfer() == ModeOfTransfer.ASYNC;
    }

    @Override
    @Transactional
    public PaymentResult handle(PaymentCommand command) {
        // For the initial submission, we are in a synchronous context.
        JourneySpecification spec = CommandProcessingContextHolder.getContext()
                .map(CommandProcessingContext::getJourneySpecification)
                .orElseThrow(() -> new IllegalStateException("JourneySpecification not found in context for async submission"));

        BusinessPolicy policy = policyFactory.create(spec);

        String journeyIdentifier = command.getIdentifier();
        Payment payment = Payment.startNew(command, policy, journeyIdentifier);
        paymentRepository.save(payment);

        var context = orchestrationContextMapper.toContext(payment.getId(), command);
        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(AsyncProcessEvent.SUBMIT);

        return PaymentResult.from(payment);
    }

    @Transactional
    public void processCreditLegResult(CreditLegResult result, UUID transactionId) {
        handleAsyncEvent(transactionId, (payment, command) -> {
            payment.recordCredit(result, buildMetadata(payment, command));

            return result.status() == CreditLegResult.CreditLegStatus.SUCCESSFUL ?
                    AsyncProcessEvent.CREDIT_LEG_SUCCEEDED : AsyncProcessEvent.CREDIT_LEG_FAILED;
        });
    }

    @Transactional
    public void processDebitLegResult(DebitLegResult result, UUID transactionId) {
        handleAsyncEvent(transactionId, (payment, command) -> {
            payment.recordDebit(result, buildMetadata(payment, command));
            return switch (result.status()) {
                case SUCCESSFUL -> AsyncProcessEvent.DEBIT_LEG_SUCCEEDED;
                case FAILED -> AsyncProcessEvent.DEBIT_LEG_FAILED;

                default -> throw new IllegalStateException("Unexpected status for debit leg result: " + result.status());
            };
        });
    }

    @Transactional
    public void processLimitEarmarkResult(LimitEarmarkResult result, UUID transactionId) {
        handleAsyncEvent(transactionId, (payment, command) -> {
            payment.recordLimitEarmark(result, buildMetadata(payment, command));

            return switch (result.status()) {
                case SUCCESSFUL -> AsyncProcessEvent.LIMIT_EARMARK_SUCCEEDED;
                case FAILED -> AsyncProcessEvent.LIMIT_EARMARK_FAILED;


                default -> throw new IllegalStateException("Unexpected status for limit earmark result: " + result.status());
            };
        });
    }

    @Transactional
    public void processDebitReversalResult(DebitLegResult result, UUID transactionId) {
        handleAsyncEvent(transactionId, (payment, command) -> {
            payment.recordDebitReversal(result, buildMetadata(payment, command));

            return switch (result.status()) {
                case REVERSAL_SUCCESSFUL -> AsyncProcessEvent.DEBIT_LEG_REVERSAL_SUCCEEDED;
                case REVERSAL_FAILED -> AsyncProcessEvent.DEBIT_LEG_REVERSAL_FAILED;


                default -> throw new IllegalStateException("Unexpected status for debit reversal result: " + result.status());
            };
        });
    }

    @Transactional
    public void processLimitReversalResult(LimitEarmarkResult result, UUID transactionId) {
        handleAsyncEvent(transactionId, (payment, command) -> {
            payment.recordLimitReversal(result, buildMetadata(payment, command));

            return switch (result.status()) {
                case REVERSAL_SUCCESSFUL -> AsyncProcessEvent.LIMIT_EARMARK_REVERSAL_SUCCEEDED;
                case REVERSAL_FAILED -> AsyncProcessEvent.LIMIT_EARMARK_REVERSAL_FAILED;


                default -> throw new IllegalStateException("Unexpected status for limit reversal result: " + result.status());
            };
        });
    }

    private void handleAsyncEvent(UUID transactionId, BiFunction<Payment, PaymentCommand, AsyncProcessEvent> handler) {
        // 1. Rehydrate Aggregate
        Payment.PaymentMemento memento = paymentRepository.findMementoById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + transactionId));

        BusinessPolicy policy = configurationPort
                .findForJourney(memento.journeyName())
                .map(policyFactory::create)
                .orElseThrow(() -> new IllegalStateException("No journey configured for identifier: " + memento.journeyName()));

        Payment payment = Payment.rehydrate(memento, policy);

        // 2. Acquire State Machine
        stateMachineFactory.acquireStateMachine(transactionId.toString()).ifPresentOrElse(
                stateMachine -> {
                    var command = orchestrationContextMapper.toCommand(stateMachine.getContext());
                    // 3. Execute logic
                    AsyncProcessEvent event = handler.apply(payment, command);

                    // 4. Commit Unit of Work
                    paymentRepository.update(payment);
                    eventDispatcher.dispatch(payment.pullDomainEvents());

                    stateMachine.fire(event);
                },
                () -> log.error("Could not acquire state machine for transaction id: {}", transactionId)
        );
    }

    private Map<String, Object> buildMetadata(Payment payment, PaymentCommand command) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("webhookUrl", command.getWebhookUrl());
        metadata.put("realtime", command.getRealtime());
        metadata.put("transactionReference", payment.getTransactionReference());
        return metadata;
    }
}
