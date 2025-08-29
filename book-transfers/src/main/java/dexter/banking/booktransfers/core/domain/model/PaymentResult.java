package dexter.banking.booktransfers.core.domain.model;

import dexter.banking.booktransfers.core.domain.primitives.ValueObject;

import java.util.UUID;

public record PaymentResult(
    UUID transactionId,
    String transactionReference,
    Status status,
    PaymentState state
) implements ValueObject {
    public static PaymentResult from(Payment payment) {
        return new PaymentResult(
            payment.getId(),
            payment.getTransactionReference(),
            payment.getStatus(),
            payment.getState()
        );
    }
}
