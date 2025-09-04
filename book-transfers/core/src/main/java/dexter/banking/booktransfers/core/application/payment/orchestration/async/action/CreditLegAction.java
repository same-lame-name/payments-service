package dexter.banking.booktransfers.core.application.payment.orchestration.async.action;

import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.TransactionLegPort;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component("creditLegAction")
@RequiredArgsConstructor
public class CreditLegAction implements SagaAction<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> {

    private final TransactionLegPort transactionLegPort;

    @Override
    public Optional<AsyncProcessEvent> apply(AsyncTransactionContext context, AsyncProcessEvent event) {
        var request = new CreditCardPort.SubmitCreditCardPaymentRequest(context.getPaymentId(), context.getCardNumber());
        transactionLegPort.sendCreditCardRequest(request);
        return Optional.empty();
    }

    @Override
    public Optional<AsyncProcessEvent> compensate(AsyncTransactionContext context, AsyncProcessEvent event) {
        // No compensation for the final leg in this SAGA model.
        return Optional.empty();
    }
}
