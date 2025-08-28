package dexter.banking.booktransfers.core.domain.model;

import java.util.UUID;

/**
 * A pure, immutable Data Transfer Object (DTO) representing the result of a payment operation.
 * Its sole purpose is to carry data out of the core domain to the calling adapter.
 * It contains no business logic.
 */
public record PaymentResult(
    UUID transactionId,
    String transactionReference,
    Status status,
    TransactionState state
) {
    /**
     * A factory method to create a result DTO from the Payment aggregate root.
     * This is a clean way to handle the mapping at the boundary of the domain.
     *
     * @param payment The source Payment aggregate.
     * @return A new, immutable PaymentResult.
     */
    public static PaymentResult from(Payment payment) {
        return new PaymentResult(
            payment.getTransactionId(),
            payment.getTransactionReference(),
            payment.getStatus(),
            payment.getState()
        );
    }
}
