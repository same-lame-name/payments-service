package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.commandbus.Command;

import java.util.UUID;

public record ResumePaymentCommand(
        UUID paymentId
) implements Command<Void> {
}
