package dexter.banking.booktransfers.core.application.payment.orchestration.sync.action;


import dexter.banking.booktransfers.core.application.payment.orchestration.sync.component.TransactionContext;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.model.ProcessEvent;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.model.ProcessState;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.CreditLegResult;
import dexter.banking.booktransfers.core.port.out.CreditCardPort;
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

    @Override
    public Optional<ProcessEvent> apply(TransactionContext context, ProcessEvent event) {
        var payment = context.getPayment();

        try {
            CreditLegResult result = creditCardPort.submitCreditCardPayment(context.getRequest());
            payment.recordCredit(result, Collections.emptyMap());
            if (result.status() == CreditLegResult.CreditLegStatus.SUCCESSFUL) {
                return Optional.of(ProcessEvent.CREDIT_LEG_SUCCEEDED);
            } else {
                return Optional.of(ProcessEvent.CREDIT_LEG_FAILED);
            }
        } catch (Exception e) {
            log.error("Sync Credit Leg failed with exception", e);
            payment.recordCredit(new CreditLegResult(null, CreditLegResult.CreditLegStatus.FAILED), Collections.emptyMap());
            return Optional.of(ProcessEvent.CREDIT_LEG_FAILED);
        }
    }

    @Override
    public Optional<ProcessEvent> compensate(TransactionContext context, ProcessEvent event) {
        return Optional.empty();
    }
}
