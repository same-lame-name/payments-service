package dexter.banking.booktransfers.core.usecase.payment.capability;

import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;

public interface ConcludePaymentRemediationUseCase {
    void handleRemediation(PaymentCommand command);
}
