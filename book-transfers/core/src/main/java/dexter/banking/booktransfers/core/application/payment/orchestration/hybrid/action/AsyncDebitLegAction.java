package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.action;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.component.HybridContextMapper;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.port.in.payment.DebitFundsUseCase;
import dexter.banking.booktransfers.core.port.in.payment.DebitReversalUseCase;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AsyncDebitLegAction implements SagaAction<ProcessStateV3, ProcessEventV3, HybridTransactionContext> {
    private final DebitFundsUseCase debitFundsUseCase;
    private final DebitReversalUseCase debitReversalUseCase;
    private final HybridContextMapper contextMapper;

    @Override
    public Optional<ProcessEventV3> apply(HybridTransactionContext context, ProcessEventV3 event) {
        PaymentCommand legacyCommand = contextMapper.mapToLegacyCommand(context);
        debitFundsUseCase.apply(legacyCommand);
        return Optional.empty();
    }

    @Override
    public Optional<ProcessEventV3> compensate(HybridTransactionContext context, ProcessEventV3 event) {
        PaymentCommand legacyCommand = contextMapper.mapToLegacyCommand(context);
        debitReversalUseCase.compensate(legacyCommand);
        return Optional.empty();
    }
}
