package dexter.banking.booktransfers.core.port.out;

/**
 * Driven Port for sending notifications to external webhooks.
 */
public interface WebhookPort {
    void notifyTransactionComplete(String webhookUrl, Object status);
}

