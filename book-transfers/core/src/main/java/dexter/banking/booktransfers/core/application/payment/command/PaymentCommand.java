package dexter.banking.booktransfers.core.application.payment.command;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.commandbus.IdempotentCommand;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

@Value
@Builder
public class PaymentCommand implements IdempotentCommand<PaymentResult> {

    @NotNull
    UUID idempotencyKey;

    @NotNull
    UUID transactionId;

    @NotBlank
    String transactionReference;
    @NotBlank
    String limitType;

    @NotBlank
    String accountNumber;
    @NotBlank
    String cardNumber;

    String webhookUrl;
    String realtime;

    @NotNull
    @Builder.Default
    ModeOfTransfer modeOfTransfer = ModeOfTransfer.ASYNC;

    // The new version discriminator field.
    @NotNull
    private final ApiVersion version;

    /**
     * The identifier is now dynamic, allowing for version-specific configuration
     * of middleware behaviors like idempotency or orchestration strategy selection.
     */
    @Override
    public String getIdentifier() {
        if (this.version == ApiVersion.V2) {
            // The identifier is now granular for V2, enabling distinct configurations.
            return "PAYMENT_SUBMIT_V2_" + this.modeOfTransfer.name();
        }
        return "PAYMENT_SUBMIT_" + this.version.name();
    }
}
