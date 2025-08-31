package dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.action;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessState;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.component.TransactionContext;
import dexter.banking.statemachine.contract.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Slf4j
public class SyncTransactionCompleteAction implements Action<ProcessState, ProcessEvent, TransactionContext> {
    @Override
    public Optional<ProcessEvent> execute(TransactionContext context, ProcessEvent event) {
        log.info("SYNC Transaction flow for {} has reached a terminal state: {}", context.getRequest().getTransactionReference(), context.getCurrentState());
        var payment = context.getPayment();

        switch(event) {
            case CREDIT_LEG_SUCCEEDED -> payment.recordPaymentSettled(buildMetadata(payment));
            case LIMIT_EARMARK_FAILED, LIMIT_EARMARK_REVERSAL_SUCCEEDED -> payment.recordPaymentFailed("", buildMetadata(payment));
            case LIMIT_EARMARK_REVERSAL_FAILED, DEBIT_LEG_REVERSAL_FAILED -> payment.recordPaymentRemediationNeeded(event.name(), buildMetadata(payment));
            default -> log.warn("No action taken for terminal event: {}", event);
        }

        return Optional.empty();
    }

    private Map<String, Object> buildMetadata(Payment payment) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("transactionReference", payment.getTransactionReference());
        return metadata;
    }
}
