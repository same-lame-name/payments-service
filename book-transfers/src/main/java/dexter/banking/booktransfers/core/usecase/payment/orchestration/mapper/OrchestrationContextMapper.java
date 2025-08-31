package dexter.banking.booktransfers.core.usecase.payment.orchestration.mapper;

import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.ModeOfTransfer;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.AsyncTransactionContext;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessState;
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
                ProcessState.NEW,
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
