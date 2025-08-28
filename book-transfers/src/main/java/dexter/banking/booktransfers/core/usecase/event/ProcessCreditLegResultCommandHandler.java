package dexter.banking.booktransfers.core.usecase.event;
import dexter.banking.booktransfers.core.port.AsyncOrchestrationEventPort;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessCreditLegResultCommandHandler implements CommandHandler<ProcessCreditLegResultCommand, Void> {

    private final AsyncOrchestrationEventPort asyncOrchestrationEventPort;
    @Override
    public Void handle(ProcessCreditLegResultCommand command) {
        asyncOrchestrationEventPort.processCreditLegResult(command.getTransactionId(), command.getResult());
        return null;
    }
}
