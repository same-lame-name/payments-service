package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.component.HybridContextMapper;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContextHolder;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.CustomerPort;
import dexter.banking.booktransfers.core.port.out.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SubmitPaymentV3CommandHandler implements CommandHandler<HighValuePaymentCommand, PaymentResult> {

    @Qualifier("v3TransactionFsmFactory")
    private final StateMachineFactory<ProcessStateV3, ProcessEventV3, HybridTransactionContext> stateMachineFactory;
    private final PaymentRepositoryPort paymentRepository;
    private final EventDispatcherPort eventDispatcher;
    private final BusinessPolicyFactory policyFactory;
    private final CustomerPort customerPort;
    private final HybridContextMapper contextMapper;

    @Override
    @Transactional
    public PaymentResult handle(HighValuePaymentCommand command) {
        if (!customerPort.isCustomerValid(command.getRelId())) {
            throw new IllegalArgumentException("Customer is not valid");
        }

        var spec = CommandProcessingContextHolder.getContext()
                .orElseThrow(() -> new IllegalStateException("JourneySpecification not found in context")).getJourneySpecification();
        BusinessPolicy policy = policyFactory.create(spec);

        var creationParams = new Payment.PaymentCreationParams(
                command.getTransactionId(),
                command.getTransactionReference()
        );
        Payment payment = Payment.startNew(creationParams, policy, command.getIdentifier());
        paymentRepository.save(payment);

        var context = contextMapper.toContext(payment.getId(), command);
        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(ProcessEventV3.SUBMIT);

        eventDispatcher.dispatch(payment.pullDomainEvents());
        return PaymentResult.from(payment);
    }
}
