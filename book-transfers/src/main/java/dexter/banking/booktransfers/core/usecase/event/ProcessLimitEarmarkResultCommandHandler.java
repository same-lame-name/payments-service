package dexter.banking.booktransfers.core.usecase.event;

import dexter.banking.booktransfers.core.port.AsyncOrchestrationEventPort;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessLimitEarmarkResultCommandHandler implements CommandHandler<ProcessLimitEarmarkResultCommand, Void> {

    private final AsyncOrchestrationEventPort asyncOrchestrationEventPort;

    @Override
    public Void handle(ProcessLimitEarmarkResultCommand command) {
        asyncOrchestrationEventPort.processLimitEarmarkResult(command.getResponse());
        return null;
    }
}
