package dexter.banking.commandbus;

import java.util.UUID;

/**
 * An interface that marks a command as supporting idempotency.
 * Any command that can be safely retried by an external client should implement this interface.
 *
 * @param <R> The type of the result returned by the command.
 */
public interface IdempotentCommand<R> extends Command<R> {

    /**
     * Returns the unique, client-generated key that identifies this specific operation.
     *
     * @return The idempotency key.
     */
    UUID getIdempotencyKey();
}
