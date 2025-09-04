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
    public AsyncTransactionContext toNewContext(UUID paymentId, PaymentCommand command) {
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
}
