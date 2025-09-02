package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.RelId;
import dexter.banking.booktransfers.core.domain.payment.valueobject.TransactionAmount;
import dexter.banking.commandbus.IdempotentCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class HighValuePaymentCommand implements IdempotentCommand<PaymentResult> {
    @NotNull
    UUID idempotencyKey;
    @NotNull
    UUID transactionId;
    @NotBlank
    String transactionReference;
    @NotNull
    RelId relId;
    @NotNull
    TransactionAmount transactionAmount;

    @NotBlank
    String limitType;
    @NotBlank
    String accountNumber;
    @NotBlank
    String cardNumber;


    @Override
    public String getIdentifier() {
        return "PAYMENT_SUBMIT_" + ApiVersion.V3.name();
    }
}
