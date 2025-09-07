package dexter.banking.booktransfers.core.port.in.payment;

public interface ConcludePaymentRemediationUseCase {
    void handleRemediation(ConcludePaymentParams params);
}
