package dexter.banking.booktransfers.core.domain.payment.exception;

/**
 * A pure domain exception thrown when a transaction cannot be found.
 * It is free of any framework annotations. The infrastructure layer will be responsible
 * for catching this and translating it into a technology-specific error,
 * like an HTTP 404 response.
 */
public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String message) {
        super(message);
    }
}

