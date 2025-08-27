package dexter.banking.commandbus;

/**
 * Represents a command to be executed. A command is an immutable object that encapsulates
 * the intent to perform an action.
 *
 * @param <R> The type of the result returned upon successful execution of the command.
 * Use {@link Void} for commands that do not return a value.
 */
public interface Command<R> {
    /**
     * Executes this command using the provided command bus.
     * This is a convenience method that delegates to {@code commandBus.send(this)}.
     *
     * @param commandBus The command bus to dispatch this command.
     * @return The result of the command execution.
     */
    default R execute(CommandBus commandBus) {
        return commandBus.send(this);
    }

    /**
     * Returns a unique, constant string identifier for this command's "flavor".
     * This is used by the configuration-driven pipeline to look up specific
     * processing rules and behaviors.
     *
     * @return A string identifier (e.g., "PAYMENT_SUBMIT").
     */
    default String getIdentifier() {
        return this.getClass().getSimpleName();
    }
}
