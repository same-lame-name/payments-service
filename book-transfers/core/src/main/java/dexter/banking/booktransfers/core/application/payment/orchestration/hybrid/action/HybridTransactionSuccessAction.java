package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.action;


import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentParams;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentSuccessUseCase;
import dexter.banking.statemachine.contract.Action;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class HybridTransactionSuccessAction implements Action<ProcessStateV3, ProcessEventV3, HybridTransactionContext> {
    private final ConcludePaymentSuccessUseCase concludePaymentSuccessUseCase;

    @Override
    public Optional<ProcessEventV3> execute(HybridTransactionContext context, ProcessEventV3 event) {
        log.info("Transaction flow for {} has reached a terminal state: {}", context.getPaymentId(), context.getCurrentState());
        var params = new ConcludePaymentParams(context.getPaymentId(), null, Collections.emptyMap());
        concludePaymentSuccessUseCase.handleSuccess(params);
        return Optional.empty();
    }
}
