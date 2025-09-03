package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.action;
import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.component.HybridContextMapper;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.TransactionLegPort;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
@Component
@RequiredArgsConstructor
public class AsyncCreditLegAction implements SagaAction<ProcessStateV3, ProcessEventV3, HybridTransactionContext> {
    private final TransactionLegPort transactionLegPort;
    private final HybridContextMapper contextMapper;
    @Override
    public Optional<ProcessEventV3> apply(HybridTransactionContext context, ProcessEventV3 event) {
        PaymentCommand legacyCommand = contextMapper.mapToLegacyCommand(context);
        var request = new CreditCardPort.SubmitCreditCardPaymentRequest(legacyCommand.getTransactionId(), legacyCommand.getCardNumber());
        transactionLegPort.sendCreditCardRequest(request);
        return Optional.empty(); // Pause FSM to await JMS callback
    }

    @Override
    public Optional<ProcessEventV3> compensate(HybridTransactionContext context, ProcessEventV3 event) {
        return Optional.empty(); // No compensation for the final leg
    }
}
