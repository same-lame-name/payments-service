package dexter.banking.booktransfers.infrastructure.adapter.out.events.listeners;

import dexter.banking.booktransfers.core.domain.event.PaymentFailedEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentInProgressEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.port.WebhookPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.events.DomainEventListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentInProgressWebhookListener implements DomainEventListener<PaymentInProgressEvent> {

    private final WebhookPort webhookPort;

    @Override
    public void on(PaymentInProgressEvent event) {
        Map<String, Object> metadata = event.metadata();
        String webhookUrl = (String) metadata.get("webhookUrl");
        TransactionState finalState = (TransactionState) metadata.get("state");

        log.info("Payment failed for transaction {}. Notifying via webhook {} for state {}", event.aggregateId(), webhookUrl, finalState);

        if (webhookUrl != null && !webhookUrl.isBlank()) {
            webhookPort.notifyTransactionComplete(webhookUrl, finalState);
        }
    }
}
