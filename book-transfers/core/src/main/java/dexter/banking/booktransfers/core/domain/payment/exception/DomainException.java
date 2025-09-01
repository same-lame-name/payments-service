package dexter.banking.booktransfers.core.domain.payment.exception;

public abstract class DomainException extends RuntimeException {
    public DomainException(String message) {
        super(message);
    }
}
