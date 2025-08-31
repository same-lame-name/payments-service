package dexter.banking.booktransfers.core.usecase.payment.capability;

import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;

public interface ConcludePaymentFailedUseCase {
    void handleFailure(PaymentCommand command);
}
