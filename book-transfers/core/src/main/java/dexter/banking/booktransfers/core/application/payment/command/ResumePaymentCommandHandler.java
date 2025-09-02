package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.booktransfers.core.port.in.payment.ResumePaymentUseCase;
import dexter.banking.commandbus.CommandHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ResumePaymentCommandHandler implements CommandHandler<ResumePaymentCommand, Void> {
    private final ResumePaymentUseCase resumePaymentUseCase;

    @Override
    @Transactional
    public Void handle(ResumePaymentCommand command) {
        this.resumePaymentUseCase.resume(command);
        return null;
    }
}
