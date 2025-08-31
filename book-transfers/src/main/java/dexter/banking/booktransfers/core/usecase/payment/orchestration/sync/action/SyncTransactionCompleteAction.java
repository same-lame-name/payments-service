package dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.action;

import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessState;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.component.TransactionContext;
import dexter.banking.statemachine.contract.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class SyncTransactionCompleteAction implements Action<ProcessState, ProcessEvent, TransactionContext> {
    @Override
    public Optional<ProcessEvent> execute(TransactionContext context, ProcessEvent event) {
        log.info("SYNC Transaction flow for {} has reached a terminal state: {}", context.getRequest().getTransactionReference(), context.getCurrentState());
        return Optional.empty();
    }
}
