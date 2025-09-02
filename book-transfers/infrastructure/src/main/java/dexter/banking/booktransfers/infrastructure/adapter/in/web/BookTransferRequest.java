package dexter.banking.booktransfers.infrastructure.adapter.in.web;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.UUID;

@Data
class BookTransferRequest {

    @NotNull(message = "idempotencyKey must not be null")
    private UUID idempotencyKey;

    @NotBlank
    String transactionReference;
    @NotBlank
    String limitType;

    @NotBlank
    String accountNumber;

    @NotBlank
    String cardNumber;

    String webhookUrl = "";

    String realtime = "";

    @Pattern(regexp = "SYNC|ASYNC", message = "Mode of transfer must be one of DIRECT, SYNC, or ASYNC")
    String modeOfTransfer = "SYNC";
}
