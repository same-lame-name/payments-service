package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.component;

import dexter.banking.booktransfers.core.application.payment.command.HighValuePaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class HybridContextMapper {

    public HybridTransactionContext toNewContext(UUID paymentId, HighValuePaymentCommand command) {
        return new HybridTransactionContext(
                paymentId,
                ProcessStateV3.NEW,
                command.getIdempotencyKey(),
                command.getTransactionReference(),
                command.getRelId(),
                command.getTransactionAmount(),
                command.getLimitType(),
                command.getAccountNumber(),
                command.getCardNumber()
        );
    }
}
