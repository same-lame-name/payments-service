package dexter.banking.booktransfers.core.usecase.payment.orchestration.async.action;

import dexter.banking.booktransfers.core.usecase.payment.capability.DebitFundsUseCase;
import dexter.banking.booktransfers.core.usecase.payment.capability.DebitReversalUseCase;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.mapper.OrchestrationContextMapper;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.AsyncTransactionContext;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessState;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("debitLegAction")
@RequiredArgsConstructor
public class DebitLegAction implements SagaAction<ProcessState, ProcessEvent, AsyncTransactionContext> {

    private final DebitFundsUseCase debitFundsUseCase;
    private final DebitReversalUseCase debitReversalUseCase;
    private final OrchestrationContextMapper orchestrationContextMapper;

    @Override
    public Optional<ProcessEvent> apply(AsyncTransactionContext context, ProcessEvent event) {
        var command = orchestrationContextMapper.toCommand(context);
        debitFundsUseCase.apply(command);
        return Optional.empty();
    }

    @Override
    public Optional<ProcessEvent> compensate(AsyncTransactionContext context, ProcessEvent event) {
        var command = orchestrationContextMapper.toCommand(context);
        debitReversalUseCase.compensate(command);
        return Optional.empty();
    }
}
