package dexter.banking.booktransfers.core.port.in.payment;

import dexter.banking.booktransfers.core.application.payment.command.ResumePaymentCommand;

public interface ResumePaymentUseCase {
    void resume(ResumePaymentCommand command);
}
