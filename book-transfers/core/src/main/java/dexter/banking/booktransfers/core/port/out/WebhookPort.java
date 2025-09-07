package dexter.banking.booktransfers.core.port.out;

import dexter.banking.booktransfers.core.domain.payment.PaymentState;

/**
 * Driven Port for sending notifications to external webhooks.
 */
public interface WebhookPort {
    void notifyTransactionStatus(String webhookUrl, WebhookNotification notification);

    /**
     * A type-safe, explicit contract for the webhook notification payload.
     * @param transactionReference The business reference for the transaction.
     * @param status The final state of the payment.
     */
    record WebhookNotification(String transactionReference, PaymentState status) {}
}
