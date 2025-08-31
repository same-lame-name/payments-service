package dexter.banking.booktransfers.core.usecase.payment.orchestration.async.action;

import dexter.banking.booktransfers.core.usecase.payment.capability.DebitFundsUseCase;
import dexter.banking.booktransfers.core.usecase.payment.capability.DebitReversalUseCase;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.component.OrchestrationContextMapper;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessState;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("debitLegAction")
@RequiredArgsConstructor
public class DebitLegAction implements SagaAction<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> {

    private final DebitFundsUseCase debitFundsUseCase;
    private final DebitReversalUseCase debitReversalUseCase;
    private final OrchestrationContextMapper orchestrationContextMapper;

    @Override
    public Optional<AsyncProcessEvent> apply(AsyncTransactionContext context, AsyncProcessEvent event) {
        var command = orchestrationContextMapper.toCommand(context);
        debitFundsUseCase.apply(command);
        return Optional.empty();
    }

    @Override
    public Optional<AsyncProcessEvent> compensate(AsyncTransactionContext context, AsyncProcessEvent event) {
        var command = orchestrationContextMapper.toCommand(context);
        debitReversalUseCase.compensate(command);
        return Optional.empty();
    }
}
