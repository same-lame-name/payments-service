package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.component;

import dexter.banking.booktransfers.core.application.payment.command.HighValuePaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.UUID;

@Mapper(componentModel = "spring")
public interface HybridContextMapper {

    @Mapping(target = "currentState", constant = "NEW")
    HybridTransactionContext toNewContext(UUID paymentId, HighValuePaymentCommand command);
}
