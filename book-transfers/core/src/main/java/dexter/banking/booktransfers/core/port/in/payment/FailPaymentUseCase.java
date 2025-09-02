package dexter.banking.booktransfers.core.port.in.payment;

import dexter.banking.booktransfers.core.application.payment.command.FailPaymentCommand;

public interface FailPaymentUseCase {
    void fail(FailPaymentCommand command);
}
