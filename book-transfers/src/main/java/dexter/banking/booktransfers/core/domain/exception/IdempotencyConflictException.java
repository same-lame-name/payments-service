package dexter.banking.booktransfers.core.domain.exception;

public class IdempotencyConflictException extends DomainException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
