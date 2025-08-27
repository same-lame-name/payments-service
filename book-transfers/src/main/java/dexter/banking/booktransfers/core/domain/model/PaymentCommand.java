package dexter.banking.booktransfers.core.domain.model;
import dexter.banking.commandbus.IdempotentCommand;
import lombok.Builder;
import lombok.Value;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.UUID;
@Value
@Builder
public class PaymentCommand implements IdempotentCommand<PaymentResponse> {

    @NotNull
    UUID idempotencyKey;
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
        return "PAYMENT_SUBMIT_" + this.version.name();
    }
}
