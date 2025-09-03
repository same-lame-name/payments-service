package dexter.banking.booktransfers.core.application.payment.orchestration.sync.action;


import dexter.banking.booktransfers.core.application.payment.orchestration.sync.component.TransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.model.ProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.model.ProcessState;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.DebitLegResult;
import dexter.banking.booktransfers.core.port.out.DepositPort;
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
            var request = new DepositPort.SubmitDepositRequest(
                    context.getRequest().getTransactionId(),
                    context.getRequest().getAccountNumber()
            );
            DebitLegResult result = depositPort.submitDeposit(request);
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
            var request = new DepositPort.SubmitDepositReversalRequest(
                    payment.getId(),
                    payment.getDebitLegResult().depositId()
            );
            DebitLegResult result = depositPort.submitDepositReversal(request);
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
