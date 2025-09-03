package dexter.banking.booktransfers.core.port.in.payment;

public interface ConcludePaymentSuccessUseCase {
    void handleSuccess(ConcludePaymentParams params);
}
