package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine.action;

import dexter.banking.booktransfers.core.domain.model.TransactionEvent;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.booktransfers.core.port.CreditCardPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.CreditCardBankingStatus;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class SyncCreditLegAction implements SagaAction<TransactionState, TransactionEvent, TransactionContext> {

    private final CreditCardPort creditCardPort;
    private final TransactionRequestMapper transactionRequestMapper;
    @Override
    public Optional<TransactionEvent> apply(TransactionContext context, TransactionEvent event) {
        log.debug("Transaction: {} - Executing synchronous Credit Leg action", context.getId());
        var payment = context.getPayment();
        CreditCardBankingRequest request = transactionRequestMapper
                .toCreditCardBankingRequest(payment.getId(), context.getRequest());

        // The action's responsibility is to orchestrate: call port, then tell the aggregate.
        payment.setState(TransactionState.CREDIT_LEG_IN_PROGRESS);
        CreditCardBankingResponse response = creditCardPort.submitCreditCardPayment(request);
        payment.recordCreditResult(response);

        if (response.getStatus() == CreditCardBankingStatus.SUCCESSFUL) {
            return Optional.of(TransactionEvent.CREDIT_LEG_SUCCEEDED);
        } else {
            return Optional.of(TransactionEvent.CREDIT_LEG_FAILED);
        }
    }

    @Override
    public Optional<TransactionEvent> compensate(TransactionContext context, TransactionEvent event) {
        // Not implemented for this flow
        return Optional.empty();
    }
}
