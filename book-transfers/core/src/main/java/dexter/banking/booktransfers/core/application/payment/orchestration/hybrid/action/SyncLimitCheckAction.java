package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.action;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.component.HybridContextMapper;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessEventV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.LimitEarmarkResult;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import dexter.banking.booktransfers.core.port.out.ConfigurationPort;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import dexter.banking.statemachine.contract.SagaAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class SyncLimitCheckAction implements SagaAction<ProcessStateV3, ProcessEventV3, HybridTransactionContext> {

    private final LimitPort limitPort;
    private final PaymentRepositoryPort paymentRepository;
    private final HybridContextMapper contextMapper;

    @Override
    @Transactional
    public Optional<ProcessEventV3> apply(HybridTransactionContext context, ProcessEventV3 event) {
        Payment payment = paymentRepository.findMementoById(context.getPaymentId())
                .map(memento -> Payment.rehydrate(memento, null)) // Policy is not needed for this action
                .orElseThrow(() -> new TransactionNotFoundException("Payment not found: " + context.getPaymentId()));

        try {
            PaymentCommand legacyCommand = contextMapper.mapToLegacyCommand(context);
            LimitEarmarkResult result = limitPort.earmarkLimit(legacyCommand);
            payment.recordLimitEarmark(result, Collections.emptyMap());

            if (result.status() == LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL) {
                paymentRepository.update(payment);
                return Optional.of(ProcessEventV3.LIMIT_APPROVED);
            } else {
                paymentRepository.update(payment);
                return Optional.of(ProcessEventV3.LIMIT_REJECTED);
            }
        } catch (Exception e) {
            log.error("V3 Sync Limit Check failed with exception", e);
            payment.recordLimitEarmark(new LimitEarmarkResult(null, LimitEarmarkResult.LimitEarmarkStatus.FAILED), Collections.emptyMap());
            paymentRepository.update(payment);
            return Optional.of(ProcessEventV3.LIMIT_REJECTED);
        }
    }

    @Override
    @Transactional
    public Optional<ProcessEventV3> compensate(HybridTransactionContext context, ProcessEventV3 event) {
        Payment payment = paymentRepository.findMementoById(context.getPaymentId())
                .map(memento -> Payment.rehydrate(memento, null))
                .orElseThrow(() -> new TransactionNotFoundException("Payment not found: " + context.getPaymentId()));

        try {
            LimitEarmarkResult result = limitPort.reverseLimitEarmark(payment);
            payment.recordLimitReversal(result, Collections.emptyMap());
            paymentRepository.update(payment);

            if (result.status() == LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_SUCCESSFUL) {
                return Optional.of(ProcessEventV3.LIMIT_EARMARK_REVERSAL_SUCCEEDED);
            } else {
                return Optional.of(ProcessEventV3.LIMIT_EARMARK_REVERSAL_FAILED);
            }
        } catch (Exception e) {
            log.error("V3 Sync Limit Earmark compensation failed with exception", e);
            payment.recordLimitReversal(new LimitEarmarkResult(payment.getLimitEarmarkResult().limitId(), LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_FAILED), Collections.emptyMap());
            paymentRepository.update(payment);
            return Optional.of(ProcessEventV3.LIMIT_EARMARK_REVERSAL_FAILED);
        }
    }
}
