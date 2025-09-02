package dexter.banking.booktransfers.core.application.payment.service;


import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.in.payment.EarmarkLimitUseCase;
import dexter.banking.booktransfers.core.port.in.payment.LimitReversalUseCase;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.ConfigurationPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.booktransfers.core.port.out.TransactionLegPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EarmarkLimitService implements EarmarkLimitUseCase, LimitReversalUseCase {

    private final TransactionLegPort transactionLegPort;
    private final PaymentRepositoryPort paymentRepository;
    private final BusinessPolicyFactory policyFactory;
    private final ConfigurationPort configurationPort;

    @Override
    public void apply(PaymentCommand command) {
        transactionLegPort.sendLimitManagementRequest(command);
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

        transactionLegPort.sendLimitReversalRequest(payment);
    }
}
