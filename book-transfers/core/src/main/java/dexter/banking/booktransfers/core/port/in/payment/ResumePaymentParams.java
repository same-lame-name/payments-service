package dexter.banking.booktransfers.core.port.in.payment;

import java.util.UUID;

public record ResumePaymentParams(
        UUID paymentId
) {
}
