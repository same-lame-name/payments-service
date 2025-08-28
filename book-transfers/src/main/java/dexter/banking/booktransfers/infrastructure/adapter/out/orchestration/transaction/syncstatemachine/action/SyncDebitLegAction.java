package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine.action;

import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.port.DepositPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionStatusMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.model.DepositBankingStatus;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncDebitLegAction implements SagaAction<TransactionState, TransactionEvent, TransactionContext> {

    private final DepositPort depositPort;
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionStatusMapper transactionStatusMapper;

    @Override
    public Optional<TransactionEvent> apply(TransactionContext context, TransactionEvent event) {
        log.debug("Transaction: {} - Executing synchronous Debit Leg action", context.getId());
        var payment = context.getPayment();
        DepositBankingRequest request = transactionRequestMapper
                .toDepositBankingRequest(payment.getId(), context.getRequest());

        payment.setState(TransactionState.DEBIT_LEG_IN_PROGRESS);
        DepositBankingResponse response = depositPort.submitDeposit(request);
        payment.recordDebitResult(response);

        if (response.getStatus() == DepositBankingStatus.SUCCESSFUL) {
            return Optional.of(TransactionEvent.DEBIT_LEG_SUCCEEDED);
        } else {
            return Optional.of(TransactionEvent.DEBIT_LEG_FAILED);
        }
    }

    @Override
    public Optional<TransactionEvent> compensate(TransactionContext context, TransactionEvent event) {
        log.debug("Transaction: {} - Compensating synchronous Debit Leg action", context.getId());
        var payment = context.getPayment();
        DepositBankingReversalRequest request = transactionStatusMapper
                .toDepositReversalRequest(payment.getId(), payment);

        payment.setState(TransactionState.DEBIT_LEG_REVERSAL_IN_PROGRESS);
        DepositBankingResponse response = depositPort.submitDepositReversal(payment.getDepositBankingResponse().getDepositId(), request);
        payment.recordDebitReversalResult(response);

        if (response.getStatus() == DepositBankingStatus.REVERSAL_SUCCESSFUL) {
            return Optional.of(TransactionEvent.DEBIT_LEG_REVERSAL_SUCCEEDED);
        } else {
            return Optional.of(TransactionEvent.DEBIT_LEG_REVERSAL_FAILED);
        }
    }
}
