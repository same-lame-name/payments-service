package dexter.banking.booktransfers.core.domain.model;

import java.util.UUID;

/**
 * A pure, technology-agnostic data record representing the state of an idempotent operation.
 * This is the contract used by the IdempotencyPort.
 *
 * @param key The unique key identifying the operation.
 * @param status The current status of the operation (STARTED or COMPLETED).
 * @param response The saved response of a COMPLETED operation.
 */
public record IdempotencyData(
    UUID key,
    IdempotencyStatus status,
    Object response
) {}
