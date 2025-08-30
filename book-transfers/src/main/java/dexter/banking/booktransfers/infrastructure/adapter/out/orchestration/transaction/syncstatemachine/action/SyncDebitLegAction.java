package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine.action;

import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.port.DepositPort;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionStatusMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.commandbus.CommandBus;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingReversalRequest;
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
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionStatusMapper transactionStatusMapper;
    private final EventDispatcherPort eventDispatcher;
    private final PaymentRepositoryPort paymentRepository;

    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();
        DepositBankingRequest request = transactionRequestMapper.toDepositBankingRequest(payment.getId(), context.getRequest());

        try {
            DebitLegResult result = depositPort.submitDeposit(request);
            payment.recordDebit(result, Collections.emptyMap());

            ProcessEvent nextEvent = (result.status() == DebitLegResult.DebitLegStatus.SUCCESSFUL)
                    ? ProcessEvent.DEBIT_LEG_SUCCEEDED
                    : ProcessEvent.DEBIT_LEG_FAILED;

            paymentRepository.update(payment);
            eventDispatcher.dispatch(payment.pullDomainEvents());

            return Optional.of(nextEvent);
        } catch (Exception e) {
            log.error("Sync Debit Leg failed with exception", e);
            payment.recordDebit(new DebitLegResult(null, DebitLegResult.DebitLegStatus.FAILED), Collections.emptyMap());
            return Optional.of(ProcessEvent.DEBIT_LEG_FAILED);
        }
    }

    @Override
    public Optional<ProcessEvent> compensate(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();
        DepositBankingReversalRequest request = transactionStatusMapper.toDepositReversalRequest(payment.getId(), payment);

        try {
            DebitLegResult result = depositPort.submitDepositReversal(payment.getDebitLegResult().depositId(), request);
            payment.recordDebitReversal(result, Collections.emptyMap());

            ProcessEvent nextEvent = (result.status() == DebitLegResult.DebitLegStatus.REVERSAL_SUCCESSFUL)
                    ? ProcessEvent.DEBIT_LEG_REVERSAL_SUCCEEDED
                    : ProcessEvent.DEBIT_LEG_REVERSAL_FAILED;

            paymentRepository.update(payment);
            eventDispatcher.dispatch(payment.pullDomainEvents());

            return Optional.of(nextEvent);
        } catch (Exception e) {
            log.error("Sync Debit Leg compensation failed with exception", e);
            payment.recordDebitReversal(new DebitLegResult(null, DebitLegResult.DebitLegStatus.REVERSAL_FAILED), Collections.emptyMap());
            return Optional.of(ProcessEvent.DEBIT_LEG_REVERSAL_FAILED);
        }
    }
}
