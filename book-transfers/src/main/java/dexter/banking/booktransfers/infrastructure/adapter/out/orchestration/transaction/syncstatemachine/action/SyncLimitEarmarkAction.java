package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine.action;

import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.LimitPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionStatusMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncLimitEarmarkAction implements SagaAction<TransactionState, TransactionEvent, TransactionContext> {

    private final LimitPort limitPort;
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionStatusMapper transactionStatusMapper;

    @Override
    public Optional<TransactionEvent> apply(TransactionContext context, TransactionEvent event) {
        log.debug("Transaction: {} - Executing synchronous Limit Earmark action", context.getId());
        var payment = context.getPayment();
        LimitManagementRequest request = transactionRequestMapper
                .toLimitManagementRequest(payment.getId(), context.getRequest());
        payment.setState(TransactionState.LIMIT_EARMARK_IN_PROGRESS);
        LimitEarmarkResult result = limitPort.earmarkLimit(request);
        payment.recordLimitEarmarkResult(result);

        if (result.status() == LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL) {
            return Optional.of(TransactionEvent.LIMIT_EARMARK_SUCCEEDED);
        } else {
            return Optional.of(TransactionEvent.LIMIT_EARMARK_FAILED);
        }
    }

    @Override
    public Optional<TransactionEvent> compensate(TransactionContext context, TransactionEvent event) {
        log.debug("Transaction: {} - Compensating synchronous Limit Earmark action", context.getId());
        var payment = context.getPayment();
        LimitManagementReversalRequest request = transactionStatusMapper
                .toLimitEarmarkReversalRequest(payment.getId(), payment);
        payment.setState(TransactionState.LIMIT_EARMARK_REVERSAL_IN_PROGRESS);
        LimitEarmarkResult result = limitPort.reverseLimitEarmark(payment.getLimitEarmarkResult().limitId(), request);
        payment.recordLimitReversalResult(result);

        if (result.status() == LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_SUCCESSFUL) {
            return Optional.of(TransactionEvent.LIMIT_EARMARK_REVERSAL_SUCCEEDED);
        } else {
            return Optional.of(TransactionEvent.LIMIT_EARMARK_REVERSAL_FAILED);
        }
    }
}
