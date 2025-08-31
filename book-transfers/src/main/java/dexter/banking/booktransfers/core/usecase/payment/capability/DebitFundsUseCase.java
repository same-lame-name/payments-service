package dexter.banking.booktransfers.core.usecase.payment.capability;

import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;


public interface DebitFundsUseCase {
    void apply(PaymentCommand command);
}
