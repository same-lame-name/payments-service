package dexter.banking.booktransfers.core.usecase.payment.orchestration.async.action;

import dexter.banking.booktransfers.core.usecase.payment.capability.EarmarkLimitUseCase;
import dexter.banking.booktransfers.core.usecase.payment.capability.LimitReversalUseCase;
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

@Component("limitEarmarkAction")
@RequiredArgsConstructor
public class LimitEarmarkAction implements SagaAction<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> {

    private final EarmarkLimitUseCase earmarkLimitUseCase;
    private final LimitReversalUseCase limitReversalUseCase;
    private final OrchestrationContextMapper orchestrationContextMapper;


    @Override
    public Optional<AsyncProcessEvent> apply(AsyncTransactionContext context, AsyncProcessEvent event) {
        var command = orchestrationContextMapper.toCommand(context);
        earmarkLimitUseCase.apply(command);
        return Optional.empty();
    }

    @Override
    public Optional<AsyncProcessEvent> compensate(AsyncTransactionContext context, AsyncProcessEvent event) {
        var command = orchestrationContextMapper.toCommand(context);
        limitReversalUseCase.compensate(command);
        return Optional.empty();
    }
}
