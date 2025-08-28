package dexter.banking.booktransfers.core.usecase.event;

import dexter.banking.commandbus.Command;
import dexter.banking.model.LimitManagementResponse;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class ProcessLimitEarmarkResultCommand implements Command<Void> {
    LimitManagementResponse response;

    @Override
    public String getIdentifier() {
        return "PROCESS_LIMIT_EARMARK";
    }
}
