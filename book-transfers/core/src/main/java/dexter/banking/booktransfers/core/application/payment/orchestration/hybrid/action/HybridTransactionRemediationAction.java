package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.action;


import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.OrchestrationContextMapper;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.component.HybridContextMapper;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentRemediationUseCase;
import dexter.banking.statemachine.contract.Action;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class HybridTransactionRemediationAction implements Action<ProcessStateV3, ProcessEventV3, HybridTransactionContext> {
    private final ConcludePaymentRemediationUseCase concludePaymentRemediationUseCase;
    private final HybridContextMapper orchestrationContextMapper;

    @Override
    public Optional<ProcessEventV3> execute(HybridTransactionContext context, ProcessEventV3 event) {
        log.info("Transaction flow for {} has reached a terminal state: {}", context.getPaymentId(), context.getCurrentState());
        var command = orchestrationContextMapper.mapToLegacyCommand(context);
        concludePaymentRemediationUseCase.handleRemediation(command);
        return Optional.empty();
    }

}
