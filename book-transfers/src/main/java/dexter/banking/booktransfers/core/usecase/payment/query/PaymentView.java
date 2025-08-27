package dexter.banking.booktransfers.core.usecase.payment.query;

import dexter.banking.booktransfers.core.domain.model.Status;
import dexter.banking.booktransfers.core.domain.model.TransactionState;

import java.util.UUID;

/**
 * The dedicated Read Model for payment queries.
 * This is an immutable, flat DTO containing only the data needed by query clients.
 * It has no business logic.
 */
public record PaymentView(
    UUID transactionId,
    String transactionReference,
    Status status,
    TransactionState state
) {}
