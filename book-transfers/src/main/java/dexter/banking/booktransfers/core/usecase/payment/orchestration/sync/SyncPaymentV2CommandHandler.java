package dexter.banking.booktransfers.core.usecase.payment.orchestration.sync;
import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.domain.model.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.model.config.CommandProcessingContext;
import dexter.banking.booktransfers.core.domain.model.config.CommandProcessingContextHolder;
import dexter.banking.booktransfers.core.port.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessState;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.component.TransactionContext;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.statemachine.StateMachineFactory;
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
        Payment payment = Payment.startNew(command, policy, journeyIdentifier);

        // For the sync machine, the live aggregate is passed in the context and mutated in memory.
        var context = new TransactionContext(payment, command);

        // The transaction boundary is this handler method. We save the initial state.
        paymentRepository.save(payment);

        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(ProcessEvent.SUBMIT); // This blocks until the FSM reaches a terminal state

        // The 'payment' object inside the context has been mutated by the FSM actions.
        // We save the final state of the aggregate at the end of the transaction.
        paymentRepository.update(payment);
        eventDispatcher.dispatch(payment.pullDomainEvents());

        return PaymentResult.from(context.getPayment());
    }
}
