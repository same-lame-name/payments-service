package dexter.banking.booktransfers.infrastructure.adapter.in.cli;

import dexter.banking.booktransfers.core.domain.model.*;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.core.usecase.payment.query.PaymentQueryUseCase;
import dexter.banking.booktransfers.core.usecase.payment.query.PaymentView;
import dexter.banking.commandbus.CommandBus;
import lombok.RequiredArgsConstructor;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@ShellComponent
@RequiredArgsConstructor
public class PaymentCommands {

    private final CommandBus commandBus;
    private final PaymentQueryUseCase paymentQueryUseCase;

    @ShellMethod(key = "submit-payment-v1", value = "Submits a new payment transaction via the V1 procedural flow.")
    public String submitPaymentV1(
            @ShellOption(help = "The reference for transaction") String transactionReference,
            @ShellOption(help = "The type of limit to earmark") String limitType,
            @ShellOption(help = "The source card number to debit.") String cardNumber,
            @ShellOption(help = "The destination account number to credit.") String accountNumber,
            @ShellOption(help = "Optional idempotency key for the request.", defaultValue = ShellOption.NULL) String idempotencyKey,
            @ShellOption(help = "Optional webhook URL for notifications.", defaultValue = ShellOption.NULL) String webhookUrl
    ) {
        PaymentCommand command = PaymentCommand.builder()
                .idempotencyKey(idempotencyKey != null ? UUID.fromString(idempotencyKey) : UUID.randomUUID())
                .transactionReference(transactionReference)
                .limitType(limitType)
                .cardNumber(cardNumber)
                .accountNumber(accountNumber)
                .webhookUrl(webhookUrl)
                .version(ApiVersion.V1)
                .build();

        PaymentResult response = command.execute(commandBus);
        return String.format("✅ V1 Payment submission accepted. Transaction ID: %s", response.transactionId());
    }

    @ShellMethod(key = "submit-payment-v2", value = "Submits a new payment transaction via the V2 orchestrated flow.")
    public String submitPaymentV2(
            @ShellOption(help = "The reference for transaction") String transactionReference,
            @ShellOption(help = "The type of limit to earmark") String limitType,
            @ShellOption(help = "The source card number to debit.") String cardNumber,
            @ShellOption(help = "The destination account number to credit.") String accountNumber,
            @ShellOption(help = "Orchestration mode: SYNC, ASYNC.", defaultValue = "ASYNC") String modeOfTransfer,
            @ShellOption(help = "Optional idempotency key for the request.", defaultValue = ShellOption.NULL) String idempotencyKey,
            @ShellOption(help = "Toggles realtime webhook notifications.", defaultValue = "false") String realtime,
            @ShellOption(help = "Optional webhook URL for notifications.", defaultValue = ShellOption.NULL) String webhookUrl
    ) {
        PaymentCommand command = PaymentCommand.builder()
                .idempotencyKey(idempotencyKey != null ? UUID.fromString(idempotencyKey) : UUID.randomUUID())
                .transactionReference(transactionReference)
                .limitType(limitType)
                .cardNumber(cardNumber)
                .accountNumber(accountNumber)
                .modeOfTransfer(ModeOfTransfer.valueOf(modeOfTransfer.toUpperCase()))
                .realtime(realtime)
                .webhookUrl(webhookUrl)
                .version(ApiVersion.V2)
                .build();

        PaymentResult response = command.execute(commandBus);
        return String.format("✅ V2 Payment submission accepted via %s. Transaction ID: %s", modeOfTransfer, response.transactionId());
    }

    @ShellMethod(key = "get-payment-by-id", value = "Gets the status of a payment by its system ID.")
    public String getTransactionById(
            @ShellOption(help = "The UUID of the transaction.") UUID transactionId
    ) {
        return paymentQueryUseCase.findById(transactionId)
                .map(this::formatPaymentView)
                .orElse("❌ Transaction not found for ID: " + transactionId);
    }

    @ShellMethod(key = "get-payment-by-ref", value = "Gets payment status(es) by its business reference.")
    public String getTransactionByRef(
            @ShellOption(help = "The business reference of the transaction (e.g., an order ID).") String reference
    ) {
        List<PaymentView> results = paymentQueryUseCase.findByReference(reference);
        if (results.isEmpty()) {
            return "❌ No transactions found for reference: " + reference;
        }
        return results.stream()
                .map(this::formatPaymentView)
                .collect(Collectors.joining("\n---\n"));
    }

    private String formatPaymentView(PaymentView view) {
        return new StringBuilder()
                .append("--- Transaction View ---\n")
                .append("ID:         ").append(view.transactionId()).append("\n")
                .append("Reference:  ").append(view.transactionReference()).append("\n")
                .append("Status:     ").append(view.status()).append("\n")
                .append("State:      ").append(view.state()).append("\n")
                .append("------------------------")
                .toString();
    }
}
