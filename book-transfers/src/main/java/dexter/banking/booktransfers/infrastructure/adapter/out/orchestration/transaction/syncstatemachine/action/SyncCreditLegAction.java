package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.syncstatemachine.action;

import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.port.CreditCardPort;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper.TransactionRequestMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessEvent;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.ProcessState;
import dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model.TransactionContext;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncCreditLegAction implements SagaAction<ProcessState, ProcessEvent, TransactionContext> {

    private final CreditCardPort creditCardPort;
    private final TransactionRequestMapper transactionRequestMapper;
    private final EventDispatcherPort eventDispatcher;
    private final PaymentRepositoryPort paymentRepository;

    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();
        CreditCardBankingRequest request = transactionRequestMapper.toCreditCardBankingRequest(payment.getId(), context.getRequest());

        try {
            CreditLegResult result = creditCardPort.submitCreditCardPayment(request);
            payment.recordCredit(result, Collections.emptyMap());

            ProcessEvent nextEvent = (result.status() == CreditLegResult.CreditLegStatus.SUCCESSFUL)
                    ? ProcessEvent.CREDIT_LEG_SUCCEEDED
                    : ProcessEvent.CREDIT_LEG_FAILED;

            paymentRepository.update(payment);
            eventDispatcher.dispatch(payment.pullDomainEvents());
            return Optional.of(nextEvent);

        } catch (Exception e) {
            log.error("Sync Credit Leg failed with exception", e);
            payment.recordCredit(new CreditLegResult(null, CreditLegResult.CreditLegStatus.FAILED), Collections.emptyMap());
            return Optional.of(ProcessEvent.CREDIT_LEG_FAILED);
        }
    }

    @Override
    public Optional<ProcessEvent> compensate(TransactionContext context, ProcessEvent event) {
        // Not implemented for this flow; debit leg compensation is the entry point
        return Optional.empty();
    }
}
