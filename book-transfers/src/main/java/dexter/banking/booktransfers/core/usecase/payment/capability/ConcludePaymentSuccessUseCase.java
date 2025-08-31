package dexter.banking.booktransfers.core.usecase.payment.capability;

import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;

public interface ConcludePaymentSuccessUseCase {
    void handleSuccess(PaymentCommand command);
}
