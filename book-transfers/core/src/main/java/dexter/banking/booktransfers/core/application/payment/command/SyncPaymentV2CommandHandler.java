package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.booktransfers.core.application.payment.orchestration.sync.component.TransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.model.ProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.model.ProcessState;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContextHolder;
import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.statemachine.StateMachineFactory;
import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContext;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SyncPaymentV2CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    private final StateMachineFactory<ProcessState, ProcessEvent, TransactionContext> stateMachineFactory;
    private final PaymentRepositoryPort paymentRepository;
    private final EventDispatcherPort eventDispatcher;
    private final BusinessPolicyFactory policyFactory;
    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V2 && command.getModeOfTransfer() == ModeOfTransfer.SYNC;
    }

    @Override
    @Transactional
    public PaymentResult handle(PaymentCommand command) {
        JourneySpecification spec = CommandProcessingContextHolder.getContext()
                .map(CommandProcessingContext::getJourneySpecification)
                .orElseThrow(() -> new IllegalStateException("JourneySpecification not found in context"));
        BusinessPolicy policy = policyFactory.create(spec);

        String journeyIdentifier = command.getIdentifier();
        var creationParams = new Payment.PaymentCreationParams(
                command.getTransactionId(),
                command.getTransactionReference()
        );
        Payment payment = Payment.startNew(creationParams, policy, journeyIdentifier);
        var context = new TransactionContext(payment, command);

        paymentRepository.save(payment);
        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(ProcessEvent.SUBMIT);

        paymentRepository.update(payment);
        eventDispatcher.dispatch(payment.pullDomainEvents());

        return PaymentResult.from(context.getPayment());
    }
}
