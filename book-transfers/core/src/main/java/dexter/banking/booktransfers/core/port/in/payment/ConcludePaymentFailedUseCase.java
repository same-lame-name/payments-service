package dexter.banking.booktransfers.core.port.in.payment;

public interface ConcludePaymentFailedUseCase {
    void handleFailure(ConcludePaymentParams params);
}
