package dexter.banking.booktransfers.core.port.in.payment;

import java.util.UUID;

public record FailPaymentParams(
        UUID paymentId,
        String reason
) {
}
