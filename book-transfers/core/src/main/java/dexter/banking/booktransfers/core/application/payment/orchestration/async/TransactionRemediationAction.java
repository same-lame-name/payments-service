package dexter.banking.booktransfers.core.application.payment.orchestration.async;


import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentParams;
import dexter.banking.booktransfers.core.port.in.payment.ConcludePaymentRemediationUseCase;
import dexter.banking.statemachine.contract.Action;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
class TransactionRemediationAction implements Action<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> {
    private final ConcludePaymentRemediationUseCase concludePaymentRemediationUseCase;
    @Override
    public Optional<AsyncProcessEvent> execute(AsyncTransactionContext context, AsyncProcessEvent event) {
        log.info("Transaction flow for {} has reached a terminal state: {}", context.getPaymentId(), context.getCurrentState());
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("webhookUrl", context.getWebhookUrl());
        metadata.put("realtime", context.getRealtime());
        var params = new ConcludePaymentParams(context.getPaymentId(), event.name(), metadata);
        concludePaymentRemediationUseCase.handleRemediation(params);
        return Optional.empty();
    }

}
