package dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.action;

import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.LimitPort;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.mapper.TransactionStatusMapper;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessState;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.TransactionContext;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncLimitEarmarkAction implements SagaAction<ProcessState, ProcessEvent, TransactionContext> {

    private final LimitPort limitPort;
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionStatusMapper transactionStatusMapper;

    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();
        LimitManagementRequest request = transactionRequestMapper.toLimitManagementRequest(payment.getId(), context.getRequest());

        try {
            LimitEarmarkResult result = limitPort.earmarkLimit(request);
            payment.recordLimitEarmark(result, Collections.emptyMap());

            if (result.status() == LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL) {
                return Optional.of(ProcessEvent.LIMIT_EARMARK_SUCCEEDED);
            } else {
                return Optional.of(ProcessEvent.LIMIT_EARMARK_FAILED);
            }
        } catch (Exception e) {
            log.error("Sync Limit Earmark failed with exception", e);
            payment.recordLimitEarmark(new LimitEarmarkResult(null, LimitEarmarkResult.LimitEarmarkStatus.FAILED), Collections.emptyMap());
            return Optional.of(ProcessEvent.LIMIT_EARMARK_FAILED);
        }
    }

    @Override
    public Optional<ProcessEvent> compensate(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();
        LimitManagementReversalRequest request = transactionStatusMapper.toLimitEarmarkReversalRequest(payment.getId(), payment);

        try {
            LimitEarmarkResult result = limitPort.reverseLimitEarmark(payment.getLimitEarmarkResult().limitId(), request);
            payment.recordLimitReversal(result, Collections.emptyMap());
            if (result.status() == LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_SUCCESSFUL) {
                return Optional.of(ProcessEvent.LIMIT_EARMARK_REVERSAL_SUCCEEDED);
            } else {
                return Optional.of(ProcessEvent.LIMIT_EARMARK_REVERSAL_FAILED);
            }
        } catch (Exception e) {
            log.error("Sync Limit Earmark compensation failed with exception", e);
            payment.recordLimitReversal(new LimitEarmarkResult(payment.getLimitEarmarkResult().limitId(), LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_FAILED), Collections.emptyMap());
            return Optional.of(ProcessEvent.LIMIT_EARMARK_REVERSAL_FAILED);
        }
    }
}
