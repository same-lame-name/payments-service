package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine.action;

import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.contract.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Slf4j
@Component
public class SyncTransactionCompleteAction implements Action<TransactionState, TransactionEvent, TransactionContext> {

    @Override
    public Optional<TransactionEvent> execute(TransactionContext context, TransactionEvent event) {
        log.debug("Transaction {} is complete with final status: {}", context.getId(), context.getCurrentState());
        return Optional.empty();
    }
}
