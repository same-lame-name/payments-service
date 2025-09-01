package dexter.banking.booktransfers.core.port.in.payment;


import dexter.banking.booktransfers.core.application.payment.PaymentCommand;

public interface DebitReversalUseCase {
    void compensate(PaymentCommand command);
}
