package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.component;

import dexter.banking.booktransfers.core.application.payment.command.HighValuePaymentCommand;
import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence.HybridTransactionContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class HybridContextMapper {

    public HybridTransactionContext toContext(UUID paymentId, HighValuePaymentCommand command) {
        return new HybridTransactionContext(
                paymentId,
                ProcessStateV3.NEW,
                command.getIdempotencyKey(),
                command.getTransactionReference(),
                command.getRelId(),
                command.getTransactionAmount(),
                command.getLimitType(),
                command.getAccountNumber(),
                command.getCardNumber()
        );
    }

    public HighValuePaymentCommand toCommand(HybridTransactionContext context) {
        return HighValuePaymentCommand.builder()
                .transactionId(context.getPaymentId())
                .idempotencyKey(context.getIdempotencyKey())
                .transactionReference(context.getTransactionReference())
                .relId(context.getRelId())
                .transactionAmount(context.getTransactionAmount())
                .limitType(context.getLimitType())
                .accountNumber(context.getAccountNumber())
                .cardNumber(context.getCardNumber())
                .build();
    }

    public PaymentCommand mapToLegacyCommand(HighValuePaymentCommand commandV3) {
        return PaymentCommand.builder()
                .transactionId(commandV3.getTransactionId())
                .idempotencyKey(commandV3.getIdempotencyKey())
                .transactionReference(commandV3.getTransactionReference())
                .version(dexter.banking.booktransfers.core.domain.payment.ApiVersion.V1) // Target V1 flow for ports
                .limitType(commandV3.getLimitType())
                .accountNumber(commandV3.getAccountNumber())
                .cardNumber(commandV3.getCardNumber())
                .build();
    }

    public PaymentCommand mapToLegacyCommand(HybridTransactionContext context) {
        return PaymentCommand.builder()
                .transactionId(context.getPaymentId())
                .idempotencyKey(context.getIdempotencyKey())
                .transactionReference(context.getTransactionReference())
                .version(dexter.banking.booktransfers.core.domain.payment.ApiVersion.V1)
                .limitType(context.getLimitType())
                .accountNumber(context.getAccountNumber())
                .cardNumber(context.getCardNumber())
                .build();
    }
}
