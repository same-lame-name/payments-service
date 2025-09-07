package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.OrchestrationContextMapper;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContext;
import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContextHolder;
import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AsyncPaymentV2CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    private final StateMachineFactory<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> stateMachineFactory;
    private final PaymentRepositoryPort paymentRepository;
    private final BusinessPolicyFactory policyFactory;
    private final OrchestrationContextMapper orchestrationContextMapper;

    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V2 && command.getModeOfTransfer() == ModeOfTransfer.ASYNC;
    }

    @Override
    @Transactional
    public PaymentResult handle(PaymentCommand command) {
        JourneySpecification spec = CommandProcessingContextHolder.getContext()
                .map(CommandProcessingContext::getJourneySpecification)
                .orElseThrow(() -> new IllegalStateException("JourneySpecification not found in context for async submission"));
        BusinessPolicy policy = policyFactory.create(spec);

        UUID transactionId = UUID.randomUUID();
        String journeyName = command.getIdentifier();

        var creationParams = new Payment.PaymentCreationParams(
                transactionId,
                command.getTransactionReference(),
                journeyName
        );

        Payment payment = Payment.startNew(creationParams, policy);
        paymentRepository.save(payment);

        var context = orchestrationContextMapper.toNewContext(payment.getId(), command);
        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(AsyncProcessEvent.SUBMIT);

        return PaymentResult.from(payment);
    }
}
