package dexter.banking.booktransfers.core.usecase.payment.orchestration.async.action;

import dexter.banking.booktransfers.core.usecase.payment.capability.EarmarkLimitUseCase;
import dexter.banking.booktransfers.core.usecase.payment.capability.LimitReversalUseCase;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.mapper.OrchestrationContextMapper;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.AsyncTransactionContext;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessState;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("limitEarmarkAction")
@RequiredArgsConstructor
public class LimitEarmarkAction implements SagaAction<ProcessState, ProcessEvent, AsyncTransactionContext> {

    private final EarmarkLimitUseCase earmarkLimitUseCase;
    private final LimitReversalUseCase limitReversalUseCase;
    private final OrchestrationContextMapper orchestrationContextMapper;


    @Override
    public Optional<ProcessEvent> apply(AsyncTransactionContext context, ProcessEvent event) {
        var command = orchestrationContextMapper.toCommand(context);
        earmarkLimitUseCase.apply(command);
        return Optional.empty();
    }

    @Override
    public Optional<ProcessEvent> compensate(AsyncTransactionContext context, ProcessEvent event) {
        var command = orchestrationContextMapper.toCommand(context);
        limitReversalUseCase.compensate(command);
        return Optional.empty();
    }
}
