package dexter.banking.booktransfers.core.usecase.payment.orchestration.sync.action;

import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.port.DepositPort;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessEvent;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.ProcessState;
import dexter.banking.booktransfers.core.usecase.payment.orchestration.model.TransactionContext;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncDebitLegAction implements SagaAction<ProcessState, ProcessEvent, TransactionContext> {

    private final DepositPort depositPort;

    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();

        try {
            DebitLegResult result = depositPort.submitDeposit(context.getRequest());
            payment.recordDebit(result, Collections.emptyMap());
            if (result.status() == DebitLegResult.DebitLegStatus.SUCCESSFUL) {
                return Optional.of(ProcessEvent.DEBIT_LEG_SUCCEEDED);
            } else {
                return Optional.of(ProcessEvent.DEBIT_LEG_FAILED);
            }
        } catch (Exception e) {
            log.error("Sync Debit Leg failed with exception", e);
            payment.recordDebit(new DebitLegResult(null, DebitLegResult.DebitLegStatus.FAILED), Collections.emptyMap());
            return Optional.of(ProcessEvent.DEBIT_LEG_FAILED);
        }
    }

    @Override
    public Optional<ProcessEvent> compensate(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();

        try {
            DebitLegResult result = depositPort.submitDepositReversal(payment);
            payment.recordDebitReversal(result, Collections.emptyMap());

            if (result.status() == DebitLegResult.DebitLegStatus.REVERSAL_SUCCESSFUL) {
                return Optional.of(ProcessEvent.DEBIT_LEG_REVERSAL_SUCCEEDED);
            } else {
                return Optional.of(ProcessEvent.DEBIT_LEG_REVERSAL_FAILED);
            }
        } catch (Exception e) {
            log.error("Sync Debit Leg compensation failed with exception", e);
            payment.recordDebitReversal(new DebitLegResult(payment.getDebitLegResult().depositId(), DebitLegResult.DebitLegStatus.REVERSAL_FAILED), Collections.emptyMap());
            return Optional.of(ProcessEvent.DEBIT_LEG_REVERSAL_FAILED);
        }
    }
}
