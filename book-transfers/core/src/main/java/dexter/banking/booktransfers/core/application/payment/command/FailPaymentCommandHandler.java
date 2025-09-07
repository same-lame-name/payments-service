package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.booktransfers.core.port.in.payment.FailPaymentParams;
import dexter.banking.booktransfers.core.port.in.payment.FailPaymentUseCase;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class FailPaymentCommandHandler implements CommandHandler<FailPaymentCommand, Void> {
    private final FailPaymentUseCase failPaymentUseCase;
    @Override
    @Transactional
    public Void handle(FailPaymentCommand command) {
        var params = new FailPaymentParams(command.paymentId(), command.reason());
        failPaymentUseCase.fail(params);
        return null;
    }
}
