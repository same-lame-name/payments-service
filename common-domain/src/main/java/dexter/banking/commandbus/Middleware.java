package dexter.banking.commandbus;

/**
 * Represents a cross-cutting concern that can be applied to the command execution pipeline.
 * Middlewares are executed in the order they are registered, forming a chain that wraps
 * the final command handler.
 */
@FunctionalInterface
public interface Middleware {
    /**
     * Intercepts the execution of a command.
     * <p>
     * The implementation of this method should decide whether to proceed with the execution
     * by calling {@code next.invoke()}, or to short-circuit the pipeline. It can also
     * perform actions before and after the next element in the chain is invoked.
     *
     * @param command The command being dispatched.
     * @param next    A closure that, when invoked, proceeds to the next middleware or the command handler.
     * @param <R>     The return type of the command.
     * @param <C>     The type of the command.
     * @return The result of the command's execution.
     */
    <R, C extends Command<R>> R invoke(C command, Next<R> next);

    /**
     * A functional interface representing the next step in the middleware chain.
     *
     * @param <T> The return type of the operation.
     */
    @FunctionalInterface
    interface Next<T> {
        /**
         * Invokes the next element in the chain.
         *
         * @return The result from the rest of the chain.
         */
        T invoke();
    }
}
