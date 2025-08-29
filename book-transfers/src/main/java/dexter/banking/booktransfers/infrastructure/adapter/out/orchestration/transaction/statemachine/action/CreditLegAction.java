package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.statemachine.action;

import dexter.banking.booktransfers.core.port.TransactionLegPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("creditLegAction")
@RequiredArgsConstructor
public class CreditLegAction implements SagaAction<ProcessState, ProcessEvent, TransactionContext> {

    private final TransactionLegPort transactionLegPort;
    private final TransactionRequestMapper transactionRequestMapper;

    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        CreditCardBankingRequest request = transactionRequestMapper.toCreditCardBankingRequest(context.getPayment().getId(), context.getRequest());
        transactionLegPort.sendCreditCardRequest(request);
        return Optional.empty();
    }

    @Override
    public Optional<ProcessEvent> compensate(TransactionContext context, ProcessEvent event) {
        // In this flow, credit leg reversal is not implemented as a separate JMS call.
        // The compensation is triggered by the failure, which then rolls back the debit leg.
        return Optional.empty();
    }
}
