package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.port.AsyncOrchestrationEventPort;
import dexter.banking.booktransfers.core.port.TransactionOrchestratorPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.component.TransactionStateMachinePersister;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.*;
import dexter.banking.statemachine.StateMachine;
import dexter.banking.statemachine.StateMachineFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.function.Consumer;

@Component("statemachine")
@Slf4j
public class TransactionOrchestratorAdapter implements TransactionOrchestratorPort, AsyncOrchestrationEventPort {

    private final StateMachineFactory<TransactionState, TransactionEvent, TransactionContext> stateMachineFactory;
    private final TransactionStateMachinePersister persister;

    public TransactionOrchestratorAdapter(
            @Qualifier("transactionFsmFactory") StateMachineFactory<TransactionState, TransactionEvent, TransactionContext> stateMachineFactory,
            TransactionStateMachinePersister persister) {
        this.stateMachineFactory = stateMachineFactory;
        this.persister = persister;
    }

    @Override
    public PaymentResult processTransaction(PaymentCommand command) {
        Payment payment = Payment.startNew(command, UUID::randomUUID);
        var context = new TransactionContext(payment, command);
        persister.saveContext(context);
        var stateMachine = stateMachineFactory.acquireStateMachine(context);
        stateMachine.fire(TransactionEvent.SUBMIT);
        return PaymentResult.from(context.getPayment());
    }

    @Override
    public void processCreditLegResult(CreditCardBankingResponse response) {

        handleAsyncEvent(response.getTransactionId(), sm -> {
            var payment = sm.getContext().getPayment();

            payment.recordCreditResult(response);
            TransactionEvent event = response.getStatus() == CreditCardBankingStatus.SUCCESSFUL ?
                    TransactionEvent.CREDIT_LEG_SUCCEEDED : TransactionEvent.CREDIT_LEG_FAILED;
            sm.fire(event);
        });
    }

    @Override
    public void processDebitLegResult(DepositBankingResponse response) {
        handleAsyncEvent(response.getTransactionId(), sm -> {
            var payment = sm.getContext().getPayment();
            payment.recordDebitResult(response);
            TransactionEvent event = response.getStatus() == DepositBankingStatus.SUCCESSFUL ?
                    TransactionEvent.DEBIT_LEG_SUCCEEDED : TransactionEvent.DEBIT_LEG_FAILED;
            sm.fire(event);
        });
    }

    @Override
    public void processLimitEarmarkResult(LimitManagementResponse response) {
        handleAsyncEvent(response.getTransactionId(), sm -> {
            var payment = sm.getContext().getPayment();
            payment.recordLimitEarmarkResult(response);
            TransactionEvent event = response.getStatus() == LimitEarmarkStatus.SUCCESSFUL ?
                    TransactionEvent.LIMIT_EARMARK_SUCCEEDED : TransactionEvent.LIMIT_EARMARK_FAILED;
            sm.fire(event);
        });
    }

    @Override
    public void processDebitLegReversalResult(DepositBankingResponse response) {
        handleAsyncEvent(response.getTransactionId(), sm -> {
            var payment = sm.getContext().getPayment();
            payment.recordDebitReversalResult(response);
            TransactionEvent event = response.getStatus() == DepositBankingStatus.REVERSAL_SUCCESSFUL ?
                    TransactionEvent.DEBIT_LEG_REVERSAL_SUCCEEDED : TransactionEvent.DEBIT_LEG_REVERSAL_FAILED;
            sm.fire(event);
        });
    }

    @Override
    public void processLimitEarmarkReversalResult(LimitManagementResponse response) {
        handleAsyncEvent(response.getTransactionId(), sm -> {
            var payment = sm.getContext().getPayment();
            payment.recordLimitReversalResult(response);
            TransactionEvent event = response.getStatus() == LimitEarmarkStatus.REVERSAL_SUCCESSFUL ?
                    TransactionEvent.LIMIT_EARMARK_REVERSAL_SUCCEEDED : TransactionEvent.LIMIT_EARMARK_REVERSAL_FAILED;
            sm.fire(event);
        });
    }

    private void handleAsyncEvent(UUID transactionId, Consumer<StateMachine<TransactionState, TransactionEvent, TransactionContext>> handler) {
        stateMachineFactory.acquireStateMachine(transactionId.toString()).ifPresentOrElse(
                handler,
                () -> log.error("Could not acquire state machine for transaction id: {}", transactionId)
        );
    }
}
