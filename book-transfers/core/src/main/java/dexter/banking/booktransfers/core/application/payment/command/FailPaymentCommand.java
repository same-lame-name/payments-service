package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.commandbus.Command;

import java.util.UUID;

public record FailPaymentCommand(
        UUID paymentId,
        String reason
) implements Command<Void> {
}
