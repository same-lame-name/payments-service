package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.action;

import dexter.banking.booktransfers.core.port.TransactionLegPort;
import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionStatusMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.statemachine.contract.SagaAction;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class LimitEarmarkAction implements SagaAction<TransactionState, TransactionEvent, TransactionContext> {

    private final TransactionLegPort transactionLegPort;
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionStatusMapper transactionStatusMapper;

    @Override
    public Optional<TransactionEvent> apply(TransactionContext context, TransactionEvent event) {
        LimitManagementRequest request = transactionRequestMapper
                .toLimitManagementRequest(context.getPayment().getId(), context.getRequest());
        transactionLegPort.sendLimitManagementRequest(request);
        log.debug("Transaction : {} - Sent Limit earmark request via MessagingPort", context.getId());
        return Optional.empty();
    }

    @Override
    public Optional<TransactionEvent> compensate(TransactionContext context, TransactionEvent event) {
        LimitManagementReversalRequest request = transactionStatusMapper
                .toLimitEarmarkReversalRequest(context.getPayment().getId(), context.getPayment());
        transactionLegPort.sendLimitReversalRequest(request);
        log.debug("Transaction: {} - Sent Limit reversal request via MessagingPort", context.getId());
        return Optional.empty();
    }

}
