package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.action;

import dexter.banking.booktransfers.core.port.TransactionLegPort;
import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionStatusMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DebitLegAction implements SagaAction<TransactionState, TransactionEvent, TransactionContext> {

    private final TransactionLegPort transactionLegPort;
    private final TransactionRequestMapper transactionRequestMapper;
    private final TransactionStatusMapper transactionStatusMapper;

    @Override
    public Optional<TransactionEvent> apply(TransactionContext context, TransactionEvent event) {
        DepositBankingRequest request = transactionRequestMapper
                .toDepositBankingRequest(context.getPayment().getId(), context.getRequest());
        transactionLegPort.sendDepositRequest(request);
        log.debug("Transaction : {} - Sent Deposit banking request via MessagingPort", context.getId());
        return Optional.empty();
    }

    @Override
    public Optional<TransactionEvent> compensate(TransactionContext context, TransactionEvent event) {
        DepositBankingReversalRequest request = transactionStatusMapper
                .toDepositReversalRequest(context.getPayment().getId(), context.getPayment());
        transactionLegPort.sendDepositReversalRequest(request);
        log.debug("Transaction : {} - Sent Deposit reversal request via MessagingPort", context.getId());
        return Optional.empty();
    }

}
