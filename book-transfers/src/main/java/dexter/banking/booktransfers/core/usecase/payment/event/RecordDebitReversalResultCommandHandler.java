package dexter.banking.booktransfers.core.usecase.payment.event;

import dexter.banking.booktransfers.core.domain.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.PaymentPolicyFactory;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecordDebitReversalResultCommandHandler implements CommandHandler<RecordDebitReversalResultCommand, Void> {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentPolicyFactory policyFactory;
    private final EventDispatcherPort eventDispatcher;

    @Override
    @Transactional
    public Void handle(RecordDebitReversalResultCommand command) {
        log.info("Handling debit leg reversal result for transaction {}", command.getTransactionId());

        Payment.PaymentMemento memento = paymentRepository.findMementoById(command.getTransactionId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + command.getTransactionId()));

        BusinessPolicy policy = policyFactory.getPolicyForJourney(memento.journeyName());

        Payment payment = Payment.rehydrate(memento, policy);

        if (command.getResult().status() == DebitLegResult.DebitLegStatus.REVERSAL_SUCCESSFUL) {
            payment.recordDebitReversalSuccess(command.getResult(), null);
        } else {
            payment.recordDebitReversalFailure(command.getResult(), null);
        }

        paymentRepository.update(payment);
        eventDispatcher.dispatch(payment.pullDomainEvents());

        return null;
    }
}
