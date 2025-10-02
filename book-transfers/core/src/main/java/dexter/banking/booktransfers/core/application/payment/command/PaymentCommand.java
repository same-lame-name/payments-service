package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.commandbus.AbstractEnrichableCommand;
import dexter.banking.commandbus.IdempotentCommand;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@RequiredArgsConstructor
@Data
public class PaymentCommand extends AbstractEnrichableCommand<PaymentResult> implements IdempotentCommand<PaymentResult> {
    @NotNull
    private final UUID idempotencyKey;
    @NotBlank
    private final String transactionReference;
    @NotBlank
    private final String limitType;
    @NotBlank
    private final String accountNumber;
    @NotBlank
    private final String cardNumber;

    private final String webhookUrl;
    private final String realtime;

    @NotNull
    private ModeOfTransfer modeOfTransfer;

    @NotNull
    private final ApiVersion version;

    @Override
    public String getIdentifier() {
        if (this.version == ApiVersion.V2) {
            return "PAYMENT_SUBMIT_V2_" + this.modeOfTransfer.name();
        }
        return "PAYMENT_SUBMIT_" + this.version.name();
    }
}
