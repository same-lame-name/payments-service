package dexter.banking.booktransfers.core.application.payment.orchestration.async.component;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * An Anti-Corruption Layer (ACL) that translates between the application-layer
 * PaymentCommand and the infrastructure-layer, persistable AsyncTransactionContext.
 */
@Component
public class OrchestrationContextMapper {

    /**
     * Flattens a PaymentCommand into a new AsyncTransactionContext for persistence.
     * @param paymentId The ID of the payment aggregate.
     * @param command The incoming command.
     * @return A new, self-contained AsyncTransactionContext.
     */
    public AsyncTransactionContext toContext(UUID paymentId, PaymentCommand command) {
        return new AsyncTransactionContext(
                paymentId,
                AsyncProcessState.NEW,
                command.getIdempotencyKey(),
                command.getTransactionReference(),
                command.getLimitType(),
                command.getAccountNumber(),
                command.getCardNumber(),
                command.getWebhookUrl(),
                command.getRealtime(),
                command.getModeOfTransfer(),
                command.getVersion()
        );
    }

    /**
     * Rehydrates a PaymentCommand from a persisted AsyncTransactionContext.
     * @param context The persisted context.
     * @return A fully re-constituted PaymentCommand.
     */
    public PaymentCommand toCommand(AsyncTransactionContext context) {
        return PaymentCommand.builder()
                .transactionId(context.getPaymentId())
                .idempotencyKey(context.getIdempotencyKey())
                .transactionReference(context.getTransactionReference())
                .limitType(context.getLimitType())
                .accountNumber(context.getAccountNumber())
                .cardNumber(context.getCardNumber())
                .webhookUrl(context.getWebhookUrl())
                .realtime(context.getRealtime())
                .modeOfTransfer(context.getModeOfTransfer())
                .version(context.getVersion())
                .build();
    }
}
