package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.action;

import dexter.banking.booktransfers.core.domain.event.PaymentInProgressEvent;
import dexter.banking.booktransfers.core.port.TransactionLegPort;
import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.statemachine.contract.SagaAction;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.CreditCardBankingRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreditLegAction implements SagaAction<TransactionState, TransactionEvent, TransactionContext> {

    private final TransactionLegPort transactionLegPort;
    private final TransactionRequestMapper transactionRequestMapper;

    @Override
    public Optional<TransactionEvent> apply(TransactionContext context, TransactionEvent event) {
        CreditCardBankingRequest request = transactionRequestMapper
                .toCreditCardBankingRequest(context.getPayment().getId(), context.getRequest());
        transactionLegPort.sendCreditCardRequest(request);
        log.debug("Transaction: {} - Sent Credit banking request via MessagingPort", context.getId());
        return Optional.empty();
    }

    @Override
    public Optional<TransactionEvent> compensate(TransactionContext context, TransactionEvent event) {
        return Optional.empty();
    }
}
