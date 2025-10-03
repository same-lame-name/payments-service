package dexter.banking.booktransfers.core.application.payment.handler;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.OrchestrationContextMapper;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.OrchestratedPaymentBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.BlueprintAccessor;
import dexter.banking.booktransfers.core.domain.shared.markers.WithJourneyContext;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AsyncPaymentV2CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    private final PaymentRepositoryPort paymentRepository;
    private final BusinessPolicyFactory policyFactory;
    private final OrchestrationContextMapper orchestrationContextMapper;

    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V2 && command.getModeOfTransfer() == ModeOfTransfer.ASYNC;
    }

    @Override
    @Transactional
    @WithJourneyContext(journeyIdentifier = "#command.getIdentifier()")
    public PaymentResult handle(PaymentCommand command) {
        OrchestratedPaymentBlueprint blueprint = command.getBlueprint();
        BusinessPolicy policy = policyFactory.create(blueprint.getPolicies());

        UUID transactionId = UUID.randomUUID();
        String journeyName = command.getIdentifier();

        var creationParams = new Payment.PaymentCreationParams(
                transactionId,
                command.getTransactionReference(),
                journeyName
        );

        Payment payment = Payment.startNew(creationParams, policy);
        paymentRepository.save(payment);

        var stateMachineFactory = blueprint.getOrchestration().getEngine();
        var context = orchestrationContextMapper.toNewContext(payment.getId(), command);
        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(AsyncProcessEvent.SUBMIT);

        return PaymentResult.from(payment);
    }
}
