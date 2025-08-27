package dexter.banking.commandbus;


/**
 * Defines the main entry point for dispatching commands.
 * The command bus is responsible for routing a command to its corresponding handler
 * and executing it, often through a chain of middleware.
 */
public interface CommandBus {
    /**
     * Sends a command to the bus for execution.
     *
     * @param command The command to be executed.
     * @param <R>     The expected return type of the command.
     * @param <C>     The type of the command.
     * @return The result of the command execution.
     * @throws CommandHandlerNotFoundException      if no handler is registered for the command.
     * @throws CommandHasMultipleHandlersException if multiple handlers are registered for the command.
     */
    <R, C extends Command<R>> R send(C command);
}
