package dexter.banking.booktransfers.core.usecase.payment;

import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.domain.model.config.CommandConfiguration;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.ConfigurationPort;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.PaymentPolicyFactory;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.port.TransactionOrchestratorPort;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SubmitPaymentV2CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    private final Map<String, TransactionOrchestratorPort> orchestratorStrategies;
    private final PaymentPolicyFactory policyFactory;
    private final ConfigurationPort configurationPort;
    private final PaymentRepositoryPort paymentRepository;

    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V2;
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

        TransactionOrchestratorPort selectedOrchestrator = findOrchestrator(command.getModeOfTransfer());
        return selectedOrchestrator.processTransaction(command, payment);
    }

    private TransactionOrchestratorPort findOrchestrator(ModeOfTransfer mode) {
        String beanName = switch (mode) {
            case SYNC -> "syncStatemachine";
            case ASYNC -> "statemachine";
        };
        return Optional.ofNullable(orchestratorStrategies.get(beanName))
                .orElseThrow(() -> new IllegalArgumentException("Invalid orchestration mode configured: '" + mode + "'"));
    }
}
