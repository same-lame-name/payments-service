package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine.action;

import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
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
