package dexter.banking.booktransfers.core.usecase.event;

import dexter.banking.booktransfers.core.domain.model.ProcessDebitLegResultCommand;
import dexter.banking.booktransfers.core.port.AsyncOrchestrationEventPort;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessDebitLegResultCommandHandler implements CommandHandler<ProcessDebitLegResultCommand, Void> {

    private final AsyncOrchestrationEventPort asyncOrchestrationEventPort;

    @Override
    public Void handle(ProcessDebitLegResultCommand command) {
        asyncOrchestrationEventPort.processDebitLegResult(command.getResponse());
        return null;
    }
}
