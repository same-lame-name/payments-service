package dexter.banking.commandbus;

import java.util.UUID;

/**
 * A marker interface for a command that supports idempotency checks.
 * The decision to perform the check is dictated by the attached journey blueprint.
 */
public interface IdempotentCommand<R> extends JourneyAwareCommand<R> {
    UUID getIdempotencyKey();
}
