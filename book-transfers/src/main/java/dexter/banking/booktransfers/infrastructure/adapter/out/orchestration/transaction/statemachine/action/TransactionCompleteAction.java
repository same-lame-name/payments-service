package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.action;

import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.contract.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
public class TransactionCompleteAction implements Action<ProcessState, ProcessEvent, TransactionContext> {
    @Override
    public Optional<ProcessEvent> execute(TransactionContext context, ProcessEvent event) {
        PaymentCommand command = context.getRequest();
        log.info("Transaction flow for {} has reached a terminal state: {}", command.getTransactionReference(), context.getCurrentState());
        // This is a terminal action. It performs no logic and does not cascade.
        // It's a placeholder for any final logging or metrics.
        return Optional.empty();
    }
}
