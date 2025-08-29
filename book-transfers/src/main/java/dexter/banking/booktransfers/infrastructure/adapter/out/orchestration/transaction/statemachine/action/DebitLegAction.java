package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.action;

import dexter.banking.booktransfers.core.port.TransactionLegPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionStatusMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("debitLegAction")
@RequiredArgsConstructor
public class DebitLegAction implements SagaAction<ProcessState, ProcessEvent, TransactionContext> {

    private final TransactionLegPort transactionLegPort;
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionStatusMapper transactionStatusMapper;


    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        DepositBankingRequest request = transactionRequestMapper.toDepositBankingRequest(context.getPayment().getId(), context.getRequest());
        transactionLegPort.sendDepositRequest(request);
        return Optional.empty();
    }

    @Override
    public Optional<ProcessEvent> compensate(TransactionContext context, ProcessEvent event) {
        DepositBankingReversalRequest request = transactionStatusMapper.toDepositReversalRequest(context.getPayment().getId(), context.getPayment());
        transactionLegPort.sendDepositReversalRequest(request);
        return Optional.empty();
    }
}
