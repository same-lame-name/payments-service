package dexter.banking.booktransfers.core.usecase.payment.event;

import dexter.banking.booktransfers.core.domain.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.port.AsyncOrchestrationEventPort;
import dexter.banking.booktransfers.core.port.EventDispatcherPort;
import dexter.banking.booktransfers.core.port.PaymentPolicyFactory;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecordCreditResultCommandHandler implements CommandHandler<RecordCreditResultCommand, Void> {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentPolicyFactory policyFactory;
    private final EventDispatcherPort eventDispatcher;
    private final AsyncOrchestrationEventPort orchestrationEventPort;

    @Override
    @Transactional
    public Void handle(RecordCreditResultCommand command) {
        log.info("Handling credit leg result for transaction {}", command.getTransactionId());

        Payment.PaymentMemento memento = paymentRepository.findMementoById(command.getTransactionId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + command.getTransactionId()));

        BusinessPolicy policy = policyFactory.getPolicyForJourney(memento.journeyName());

        Payment payment = Payment.rehydrate(memento, policy);

        payment.recordCredit(command.getResult(), Collections.emptyMap());

        paymentRepository.update(payment);
        eventDispatcher.dispatch(payment.pullDomainEvents());

        orchestrationEventPort.processCreditLegResult(command.getTransactionId(), command.getResult());

        return null;
    }
}
