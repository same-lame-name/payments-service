package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine.action;

import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.port.DepositPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionStatusMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncDebitLegAction implements SagaAction<ProcessState, ProcessEvent, TransactionContext> {

    private final DepositPort depositPort;
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionStatusMapper transactionStatusMapper;

    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();
        DepositBankingRequest request = transactionRequestMapper.toDepositBankingRequest(payment.getId(), context.getRequest());

        try {
            DebitLegResult result = depositPort.submitDeposit(request);
            if (result.status() == DebitLegResult.DebitLegStatus.SUCCESSFUL) {
                payment.recordDebitSuccess(result, null);
                return Optional.of(ProcessEvent.DEBIT_LEG_SUCCEEDED);
            } else {
                payment.recordDebitFailure(result, null);
                return Optional.of(ProcessEvent.DEBIT_LEG_FAILED);
            }
        } catch (Exception e) {
            log.error("Sync Debit Leg failed with exception", e);
            payment.recordDebitFailure(new DebitLegResult(null, DebitLegResult.DebitLegStatus.FAILED), null);
            return Optional.of(ProcessEvent.DEBIT_LEG_FAILED);
        }
    }

    @Override
    public Optional<ProcessEvent> compensate(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();
        DepositBankingReversalRequest request = transactionStatusMapper.toDepositReversalRequest(payment.getId(), payment);

        try {
            DebitLegResult result = depositPort.submitDepositReversal(payment.getDebitLegResult().depositId(), request);

            if (result.status() == DebitLegResult.DebitLegStatus.REVERSAL_SUCCESSFUL) {
                payment.recordDebitReversalSuccess(result, null);
                return Optional.of(ProcessEvent.DEBIT_LEG_REVERSAL_SUCCEEDED);
            } else {
                payment.recordDebitReversalFailure(result, null);
                return Optional.of(ProcessEvent.DEBIT_LEG_REVERSAL_FAILED);
            }
        } catch (Exception e) {
            log.error("Sync Debit Leg compensation failed with exception", e);
            payment.recordDebitReversalFailure(new DebitLegResult(payment.getDebitLegResult().depositId(), DebitLegResult.DebitLegStatus.REVERSAL_FAILED), null);
            return Optional.of(ProcessEvent.DEBIT_LEG_REVERSAL_FAILED);
        }
    }
}
