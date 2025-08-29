package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.action;

import dexter.banking.booktransfers.core.port.TransactionLegPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionStatusMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("limitEarmarkAction")
@RequiredArgsConstructor
public class LimitEarmarkAction implements SagaAction<ProcessState, ProcessEvent, TransactionContext> {

    private final TransactionLegPort transactionLegPort;
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionStatusMapper transactionStatusMapper;

    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        LimitManagementRequest request = transactionRequestMapper.toLimitManagementRequest(context.getPayment().getId(), context.getRequest());
        transactionLegPort.sendLimitManagementRequest(request);
        return Optional.empty();
    }

    @Override
    public Optional<ProcessEvent> compensate(TransactionContext context, ProcessEvent event) {
        LimitManagementReversalRequest request = transactionStatusMapper.toLimitEarmarkReversalRequest(context.getPayment().getId(), context.getPayment());
        transactionLegPort.sendLimitReversalRequest(request);
        return Optional.empty();
    }
}
