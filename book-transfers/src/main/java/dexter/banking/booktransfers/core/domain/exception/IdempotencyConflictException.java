package dexter.banking.booktransfers.core.domain.exception;

/**
 * A pure domain exception, part of the IdempotencyPort contract.
 * It is thrown when an attempt is made to process a command that is already in progress.
 */
public class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String message) {
        super(message);
    }
}
