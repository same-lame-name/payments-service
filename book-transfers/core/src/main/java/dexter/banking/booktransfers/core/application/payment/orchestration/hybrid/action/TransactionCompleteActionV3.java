package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.action;

import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.statemachine.contract.Action;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransactionCompleteActionV3 implements Action<ProcessStateV3, ProcessEventV3, HybridTransactionContext> {

    private final PaymentRepositoryPort paymentRepository;

    @Override
    @Transactional
    public Optional<ProcessEventV3> execute(HybridTransactionContext context, ProcessEventV3 event) {
        log.info("V3 Transaction flow for {} has reached a terminal state: {}", context.getTransactionReference(), context.getCurrentState());
        Payment payment = paymentRepository.findMementoById(context.getPaymentId())
                .map(memento -> Payment.rehydrate(memento, null))
                .orElseThrow(() -> new TransactionNotFoundException("Payment not found: " + context.getPaymentId()));

        switch(context.getCurrentState()) {
            case PROCESS_SETTLED -> payment.recordPaymentSettled(Collections.emptyMap());
            case PROCESS_FAILED -> payment.recordPaymentFailed(event.name(), Collections.emptyMap());
            case REMEDIATION_REQUIRED -> payment.recordPaymentRemediationNeeded(event.name(), Collections.emptyMap());
            default -> log.warn("No final action taken for state: {}", context.getCurrentState());
        }

        paymentRepository.update(payment);
        return Optional.empty();
    }
}
