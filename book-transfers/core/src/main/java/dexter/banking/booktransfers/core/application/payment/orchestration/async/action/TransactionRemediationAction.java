package dexter.banking.booktransfers.core.application.payment.orchestration.async.action;


import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentParams;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentRemediationUseCase;
import dexter.banking.statemachine.contract.Action;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionRemediationAction implements Action<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> {
    private final ConcludePaymentRemediationUseCase concludePaymentRemediationUseCase;
    @Override
    public Optional<AsyncProcessEvent> execute(AsyncTransactionContext context, AsyncProcessEvent event) {
        log.info("Transaction flow for {} has reached a terminal state: {}", context.getPaymentId(), context.getCurrentState());
        var params = new ConcludePaymentParams(context.getPaymentId(), event.name(), Collections.emptyMap());
        concludePaymentRemediationUseCase.handleRemediation(params);
        return Optional.empty();
    }

}
