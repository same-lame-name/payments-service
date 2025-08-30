package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine.action;

import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.LimitPort;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionStatusMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.commandbus.CommandBus;
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
    private final EventDispatcherPort eventDispatcher;
    private final PaymentRepositoryPort paymentRepository;

    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();
        LimitManagementRequest request = transactionRequestMapper.toLimitManagementRequest(payment.getId(), context.getRequest());

        try {
            LimitEarmarkResult result = limitPort.earmarkLimit(request);
            payment.recordLimitEarmark(result, Collections.emptyMap());

            ProcessEvent nextEvent = (result.status() == LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL)
                    ? ProcessEvent.LIMIT_EARMARK_SUCCEEDED
                    : ProcessEvent.LIMIT_EARMARK_FAILED;

            paymentRepository.update(payment);
            eventDispatcher.dispatch(payment.pullDomainEvents());

            return Optional.of(nextEvent);
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

            ProcessEvent nextEvent = (result.status() == LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_SUCCESSFUL)
                    ? ProcessEvent.LIMIT_EARMARK_REVERSAL_SUCCEEDED
                    : ProcessEvent.LIMIT_EARMARK_REVERSAL_FAILED;

            paymentRepository.update(payment);
            eventDispatcher.dispatch(payment.pullDomainEvents());

            return Optional.of(nextEvent);
        } catch (Exception e) {
            log.error("Sync Limit Earmark compensation failed with exception", e);
            payment.recordLimitReversal(new LimitEarmarkResult(null, LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_FAILED), Collections.emptyMap());
            return Optional.of(ProcessEvent.LIMIT_EARMARK_REVERSAL_FAILED);
        }
    }
}
