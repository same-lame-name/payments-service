package dexter.banking.booktransfers.core.usecase.payment.capability;

import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;


public interface DebitReversalUseCase {
    void compensate(PaymentCommand command);
}
