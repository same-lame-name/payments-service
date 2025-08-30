package dexter.banking.booktransfers.core.usecase.payment.event;

import dexter.banking.booktransfers.core.domain.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
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
public class RecordLimitEarmarkResultCommandHandler implements CommandHandler<RecordLimitEarmarkResultCommand, Void> {

    private final PaymentRepositoryPort paymentRepository;
    private final PaymentPolicyFactory policyFactory;
    private final EventDispatcherPort eventDispatcher;
    private final AsyncOrchestrationEventPort orchestrationEventPort;

    @Override
    @Transactional
    public Void handle(RecordLimitEarmarkResultCommand command) {
        log.info("Handling limit earmark result for transaction {}", command.getTransactionId());

        Payment.PaymentMemento memento = paymentRepository.findMementoById(command.getTransactionId())
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + command.getTransactionId()));

        BusinessPolicy policy = policyFactory.getPolicyForJourney(memento.journeyName());

        Payment payment = Payment.rehydrate(memento, policy);

        payment.recordLimitEarmark(command.getResult(), Collections.emptyMap());
        paymentRepository.update(payment);
        eventDispatcher.dispatch(payment.pullDomainEvents());

        orchestrationEventPort.processLimitEarmarkResult(command.getTransactionId(), command.getResult());
        return null;
    }
}
