package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.action;

import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.statemachine.contract.Action;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionCompleteAction implements Action<TransactionState, TransactionEvent, TransactionContext> {

    @Override
    public Optional<TransactionEvent> execute(TransactionContext context, TransactionEvent event) {
        log.debug("Transaction {} is complete with final event: {}", context.getId(), event);
        return Optional.empty();
    }
}
