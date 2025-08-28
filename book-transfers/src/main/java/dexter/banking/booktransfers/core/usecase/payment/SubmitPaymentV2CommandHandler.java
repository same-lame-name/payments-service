package dexter.banking.booktransfers.core.usecase.payment;

import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.model.PaymentCommand;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.port.TransactionOrchestratorPort;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SubmitPaymentV2CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    private final Map<String, TransactionOrchestratorPort> orchestratorStrategies;
    /**
     * This handler matches only V2 commands, demonstrating the content-based
     * routing feature of the command bus.
     */
    @Override
    public boolean matches(PaymentCommand command) {
        return command.getVersion() == ApiVersion.V2;
    }

    @Override
    public PaymentResult handle(PaymentCommand command) {
        // This handler now acts as a FACTORY for orchestration strategies.
        TransactionOrchestratorPort selectedOrchestrator = findOrchestrator(command.getModeOfTransfer());
        return selectedOrchestrator.processTransaction(command);
    }

    private TransactionOrchestratorPort findOrchestrator(ModeOfTransfer mode) {
        String beanName = switch (mode) {
            case SYNC -> "syncStatemachine";
            case ASYNC -> "statemachine";
            default -> throw new IllegalArgumentException("V2 transactions must use an orchestrated mode (SYNC or ASYNC), but got: '" + mode + "'");
        };
        return Optional.ofNullable(orchestratorStrategies.get(beanName))
                .orElseThrow(() -> new IllegalArgumentException("Invalid orchestration mode configured: '" + mode + "'"));
    }
}
