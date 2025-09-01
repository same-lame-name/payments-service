package dexter.banking.booktransfers.infrastructure.adapter.out.events.listeners;

import dexter.banking.booktransfers.core.domain.event.ManualInterventionRequiredEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentFailedEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentInProgressEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentSuccessfulEvent;
import dexter.banking.booktransfers.core.domain.model.PaymentState;
import dexter.banking.booktransfers.core.port.WebhookPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;

/**
 * A single, unified Spring component that listens for all domain events.
 * It uses the @TransactionalEventListener to ensure that event handling only occurs
 * AFTER the originating transaction has successfully committed. This is critical for
 * preventing inconsistent state if a webhook call fails after the DB commit.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionalDomainEventListener {

    private final WebhookPort webhookPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentSuccessfulEvent event) {
        log.info("Handling successful payment event for transaction {}", event.aggregateId());
        notifyWebhook(event.aggregateId(), event.aggregateState(), event.metadata());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentFailedEvent event) {
        log.info("Handling failed payment event for transaction {}", event.aggregateId());
        notifyWebhook(event.aggregateId(), event.aggregateState(), event.metadata());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ManualInterventionRequiredEvent event) {
        log.info("Handling manual intervention event for transaction {}", event.aggregateId());
        notifyWebhook(event.aggregateId(), event.aggregateState(), event.metadata());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentInProgressEvent event) {
        log.info("Handling in-progress payment event for transaction {}", event.aggregateId());
        var eventMetadata = event.metadata();
        boolean realtimeEnabled = eventMetadata.containsKey("realtime") && "true".equals(eventMetadata.get("realtime"));

        if (realtimeEnabled) {
            log.info("Realtime flag is set. Notifying webhook immediately for transaction {}", event.aggregateId());
            notifyWebhook(event.aggregateId(), event.aggregateState(), event.metadata());
        } else {
            log.info("Realtime flag not set or false. Skipping immediate webhook notification for transaction {}", event.aggregateId());
        }
    }

    private void notifyWebhook(UUID aggregateId, PaymentState paymentState, Map<String, Object> metadata) {
        String webhookUrl = (String) metadata.get("webhookUrl");
        String transactionReference = (String) metadata.get("transactionReference");
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            log.info("Notifying webhook {} for transaction {} with final state {}", webhookUrl, aggregateId, paymentState);
//            webhookPort.notifyTransactionComplete(webhookUrl, new PaymentNotification(transactionReference, paymentState));
            webhookPort.notifyTransactionComplete(webhookUrl, paymentState);
        } else {
            log.info("No webhook URL configured for transaction {}. Skipping notification.", aggregateId);
        }
    }

    public record PaymentNotification(String transactionReference, PaymentState status) {}
}
