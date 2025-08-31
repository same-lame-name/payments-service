package dexter.banking.booktransfers.core.usecase.payment.orchestration.async.action;

import dexter.banking.booktransfers.core.usecase.payment.capability.CreditFundsUseCase;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.mapper.OrchestrationContextMapper;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.AsyncTransactionContext;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessState;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component("creditLegAction")
@RequiredArgsConstructor
public class CreditLegAction implements SagaAction<ProcessState, ProcessEvent, AsyncTransactionContext> {

    private final CreditFundsUseCase creditFundsUseCase;
    private final OrchestrationContextMapper orchestrationContextMapper;

    @Override
    public Optional<ProcessEvent> apply(AsyncTransactionContext context, ProcessEvent event) {
        var command = orchestrationContextMapper.toCommand(context);
        creditFundsUseCase.apply(command);
        return Optional.empty();
    }

    @Override
    public Optional<ProcessEvent> compensate(AsyncTransactionContext context, ProcessEvent event) {
        // No compensation for the final leg in this SAGA model.
        return Optional.empty();
    }
}
