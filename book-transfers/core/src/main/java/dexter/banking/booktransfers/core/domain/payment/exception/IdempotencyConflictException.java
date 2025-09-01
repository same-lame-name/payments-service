package dexter.banking.booktransfers.core.domain.payment.exception;

public class IdempotencyConflictException extends DomainException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
