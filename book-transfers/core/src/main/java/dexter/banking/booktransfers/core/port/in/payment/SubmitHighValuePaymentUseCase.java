package dexter.banking.booktransfers.core.port.in.payment;

import dexter.banking.booktransfers.core.application.payment.command.HighValuePaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;

public interface SubmitHighValuePaymentUseCase {
    PaymentResult submit(HighValuePaymentCommand command);
}
