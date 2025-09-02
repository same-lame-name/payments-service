package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.port.in.payment.SubmitHighValuePaymentUseCase;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class SubmitPaymentV3CommandHandler implements CommandHandler<HighValuePaymentCommand, PaymentResult> {
    private final SubmitHighValuePaymentUseCase submitHighValuePaymentUseCase;

    @Override
    @Transactional
    public PaymentResult handle(HighValuePaymentCommand command) {
        return submitHighValuePaymentUseCase.submit(command);
    }
}
