package dexter.banking.booktransfers.core.port.in.payment;

import java.util.Map;
import java.util.UUID;

/**
 * A dedicated, pure Parameter Object for the ConcludePayment use cases.
 * It decouples the use cases from any specific command DTO.
 */
public record ConcludePaymentParams(
        UUID transactionId,
        String reason,
        Map<String, Object> metadata
) {
}
