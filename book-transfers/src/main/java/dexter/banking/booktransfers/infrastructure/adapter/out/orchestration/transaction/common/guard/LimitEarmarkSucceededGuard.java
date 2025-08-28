package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.guard;

import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.LimitEarmarkStatus;
import dexter.banking.statemachine.contract.Guard;
import org.springframework.stereotype.Component;

@Component
public class LimitEarmarkSucceededGuard implements Guard<TransactionContext, TransactionEvent> {
    @Override
    public boolean evaluate(TransactionContext context, TransactionEvent event) {
        return context.getPayment().getLimitManagementResponse() != null &&
                context.getPayment().getLimitManagementResponse().getStatus() == LimitEarmarkStatus.SUCCESSFUL;
    }
}
