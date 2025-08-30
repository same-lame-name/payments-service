package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.component;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.contract.Action;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Component
@RequiredArgsConstructor
public class SyncTransactionCompleteAction implements Action<ProcessState, ProcessEvent, TransactionContext> {
    private final EventDispatcherPort eventDispatcher;
    private final PaymentRepositoryPort paymentRepository;

    @Override
    public Optional<ProcessEvent> execute(TransactionContext context, ProcessEvent event) {
        log.info("SYNC Transaction flow for {} has reached a terminal state: {}", context.getRequest().getTransactionReference(), context.getCurrentState());

        var payment = context.getPayment();

        switch (event) {
            case CREDIT_LEG_SUCCEEDED -> payment.recordPaymentSettled(buildMetadata(context.getRequest(), payment));
            case LIMIT_EARMARK_FAILED, LIMIT_EARMARK_REVERSAL_SUCCEEDED -> payment.recordPaymentFailed("Payment compensated", buildMetadata(context.getRequest(), payment));
            case LIMIT_EARMARK_REVERSAL_FAILED, DEBIT_LEG_REVERSAL_FAILED -> payment.recordPaymentRemediationNeeded("Compensation Failed", buildMetadata(context.getRequest(), payment));
            default -> throw new IllegalStateException("Unexpected event: " + event);
        }

        paymentRepository.update(payment);
        eventDispatcher.dispatch(payment.pullDomainEvents());

        return Optional.empty();
    }

    private Map<String, Object> buildMetadata(PaymentCommand command, Payment payment) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("webhookUrl", command.getWebhookUrl());
        metadata.put("transactionReference", payment.getTransactionReference());
        return metadata;
    }
}
