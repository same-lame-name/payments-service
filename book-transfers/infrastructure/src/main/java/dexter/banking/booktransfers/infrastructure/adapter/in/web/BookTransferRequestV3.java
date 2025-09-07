package dexter.banking.booktransfers.infrastructure.adapter.in.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
class BookTransferRequestV3 {
    @NotNull(message = "idempotencyKey must not be null")
    private UUID idempotencyKey;

    @NotBlank
    String transactionReference;

    @NotNull
    private UUID relId;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotBlank
    String limitType;

    @NotBlank
    String accountNumber;

    @NotBlank
    String cardNumber;
}
