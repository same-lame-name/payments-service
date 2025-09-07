package dexter.banking.booktransfers.core.application.payment.orchestration.async.component;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

/**
 * An Anti-Corruption Layer (ACL) that translates between the application-layer
 * PaymentCommand and the infrastructure-layer, persistable AsyncTransactionContext.
 */
@Mapper(componentModel = "spring")
public interface OrchestrationContextMapper {

    /**
     * Flattens a PaymentCommand into a new AsyncTransactionContext for persistence.
     * @param paymentId The ID of the payment aggregate.
     * @param command The incoming command.
     * @return A new, self-contained AsyncTransactionContext.
     */
    @Mapping(target = "currentState", constant = "NEW")
    AsyncTransactionContext toNewContext(UUID paymentId, PaymentCommand command);
}
