package dexter.banking.booktransfers.core.usecase.event;

import dexter.banking.booktransfers.core.port.AsyncOrchestrationEventPort;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProcessLimitEarmarkReversalResultCommandHandler implements CommandHandler<ProcessLimitEarmarkReversalResultCommand, Void> {

    private final AsyncOrchestrationEventPort asyncOrchestrationEventPort;

    @Override
    public Void handle(ProcessLimitEarmarkReversalResultCommand command) {
        asyncOrchestrationEventPort.processLimitEarmarkReversalResult(command.getResponse());
        return null;
    }
}
