package dexter.banking.booktransfers.core.port.in.payment;


import dexter.banking.booktransfers.core.application.payment.PaymentCommand;

public interface ConcludePaymentRemediationUseCase {
    void handleRemediation(PaymentCommand command);
}
