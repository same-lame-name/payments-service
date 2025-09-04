package dexter.banking.booktransfers.core.application.payment.orchestration.async.action;

import dexter.banking.booktransfers.core.application.payment.orchestration.async.component.AsyncTransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.port.out.TransactionLegPort;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("limitEarmarkAction")
@RequiredArgsConstructor
public class LimitEarmarkAction implements SagaAction<AsyncProcessState, AsyncProcessEvent, AsyncTransactionContext> {

    private final TransactionLegPort transactionLegPort;
    private final PaymentRepositoryPort paymentRepository;


    @Override
    public Optional<AsyncProcessEvent> apply(AsyncTransactionContext context, AsyncProcessEvent event) {
        var request = new LimitPort.EarmarkLimitRequest(context.getPaymentId(), context.getLimitType());
        transactionLegPort.sendLimitManagementRequest(request);
        return Optional.empty();
    }

    @Override
    public Optional<AsyncProcessEvent> compensate(AsyncTransactionContext context, AsyncProcessEvent event) {
        Payment.PaymentMemento memento = paymentRepository.findMementoById(context.getPaymentId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + context.getPaymentId()));

        var request = new LimitPort.ReverseLimitEarmarkRequest(
                memento.id(),
                memento.limitEarmarkResult().limitId()
        );
        transactionLegPort.sendLimitReversalRequest(request);
        return Optional.empty();
    }
}
