package dexter.banking.booktransfers.core.usecase.payment.orchestration.async.action;

import dexter.banking.booktransfers.core.usecase.payment.capability.CreditFundsUseCase;
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

@Component("creditLegAction")
@RequiredArgsConstructor
public class CreditLegAction implements SagaAction<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> {

    private final CreditFundsUseCase creditFundsUseCase;
    private final OrchestrationContextMapper orchestrationContextMapper;

    @Override
    public Optional<AsyncProcessEvent> apply(AsyncTransactionContext context, AsyncProcessEvent event) {
        var command = orchestrationContextMapper.toCommand(context);
        creditFundsUseCase.apply(command);
        return Optional.empty();
    }

    @Override
    public Optional<AsyncProcessEvent> compensate(AsyncTransactionContext context, AsyncProcessEvent event) {
        // No compensation for the final leg in this SAGA model.
        return Optional.empty();
    }
}
