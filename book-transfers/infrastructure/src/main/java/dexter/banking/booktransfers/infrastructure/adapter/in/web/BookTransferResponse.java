package dexter.banking.booktransfers.infrastructure.adapter.in.web;

import dexter.banking.booktransfers.core.domain.payment.PaymentState;
import dexter.banking.booktransfers.core.domain.payment.Status;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Data Transfer Object for API responses.
 * This provides a stable, public-facing representation of a transaction's state,
 * decoupling the API clients from our internal domain model.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class BookTransferResponse {
    private UUID transactionId;
    private Status status;
    private PaymentState state;
}
