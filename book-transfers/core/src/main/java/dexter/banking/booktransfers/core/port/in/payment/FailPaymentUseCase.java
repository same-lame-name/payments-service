package dexter.banking.booktransfers.core.port.in.payment;

public interface FailPaymentUseCase {
    void fail(FailPaymentParams params);
}
