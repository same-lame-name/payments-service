package dexter.banking.booktransfers.core.usecase.payment.capability;

import dexter.banking.booktransfers.core.domain.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.ConfigurationPort;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.port.TransactionLegPort;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DebitFundsService implements DebitFundsUseCase, DebitReversalUseCase {

    private final TransactionLegPort transactionLegPort;
    private final PaymentRepositoryPort paymentRepository;
    private final BusinessPolicyFactory policyFactory;
    private final ConfigurationPort configurationPort;

    @Override
    public void apply(PaymentCommand command) {
        transactionLegPort.sendDepositRequest(command);
    }

    @Override
    @Transactional
    public void compensate(PaymentCommand command) {
        UUID transactionId = command.getTransactionId();
        Payment.PaymentMemento memento = paymentRepository.findMementoById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for ID: " + transactionId));

        JourneySpecification spec = configurationPort.findForJourney(memento.journeyName())
                .orElseThrow(() -> new IllegalStateException("No journey configured for identifier: " + memento.journeyName()));
        BusinessPolicy policy = policyFactory.create(spec);

        Payment payment = Payment.rehydrate(memento, policy);

        transactionLegPort.sendDepositReversalRequest(payment);
    }
}
