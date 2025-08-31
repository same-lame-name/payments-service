package dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.action;

import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.LimitPort;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.model.ProcessState;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.component.TransactionContext;
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

    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();

        try {
            LimitEarmarkResult result = limitPort.earmarkLimit(context.getRequest());
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

        try {
            LimitEarmarkResult result = limitPort.reverseLimitEarmark(payment);
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
