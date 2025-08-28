package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.guard;

import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.contract.Guard;
import org.springframework.stereotype.Component;

@Component
public class DebitLegSucceededGuard implements Guard<TransactionContext, TransactionEvent> {
    @Override
    public boolean evaluate(TransactionContext context, TransactionEvent event) {
        return context.getPayment().getDebitLegResult() != null &&
               context.getPayment().getDebitLegResult().status() == DebitLegResult.DebitLegStatus.SUCCESSFUL;
    }
}
