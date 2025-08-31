package dexter.banking.booktransfers.core.usecase.payment.orchestration.async.action;

import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.mapper.OrchestrationContextMapper;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.AsyncTransactionContext;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessState;
import dexter.banking.statemachine.contract.Action;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionCompleteAction implements Action<ProcessState, ProcessEvent, AsyncTransactionContext> {
    private final OrchestrationContextMapper orchestrationContextMapper;

    @Override
    public Optional<ProcessEvent> execute(AsyncTransactionContext context, ProcessEvent event) {
        PaymentCommand command = orchestrationContextMapper.toCommand(context);
        log.info("Transaction flow for {} has reached a terminal state: {}", command.getTransactionReference(), context.getCurrentState());
        return Optional.empty();
    }
}
