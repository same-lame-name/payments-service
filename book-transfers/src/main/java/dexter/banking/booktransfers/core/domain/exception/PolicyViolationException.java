package dexter.banking.booktransfers.core.domain.exception;

public class PolicyViolationException extends DomainException {
    public PolicyViolationException(String message) {
        super(message);
    }
}
