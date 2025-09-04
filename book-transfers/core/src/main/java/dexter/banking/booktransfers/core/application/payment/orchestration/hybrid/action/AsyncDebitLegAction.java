package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.action;

import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.port.out.TransactionLegPort;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
@RequiredArgsConstructor
public class AsyncDebitLegAction implements SagaAction<ProcessStateV3, ProcessEventV3, HybridTransactionContext> {
    private final TransactionLegPort transactionLegPort;
    private final PaymentRepositoryPort paymentRepository;

    @Override
    public Optional<ProcessEventV3> apply(HybridTransactionContext context, ProcessEventV3 event) {
        var request = new DepositPort.SubmitDepositRequest(context.getPaymentId(), context.getAccountNumber());
        transactionLegPort.sendDepositRequest(request);
        return Optional.empty();
    }

    @Override
    public Optional<ProcessEventV3> compensate(HybridTransactionContext context, ProcessEventV3 event) {
        Payment.PaymentMemento memento = paymentRepository.findMementoById(context.getPaymentId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + context.getPaymentId()));

        var request = new DepositPort.SubmitDepositReversalRequest(
                memento.id(),
                memento.debitLegResult().depositId()
        );
        transactionLegPort.sendDepositReversalRequest(request);
        return Optional.empty();
    }
}
