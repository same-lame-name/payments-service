package dexter.banking.booktransfers.core.port.in.payment;


import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;

public interface ConcludePaymentFailedUseCase {
    void handleFailure(PaymentCommand command);
}
