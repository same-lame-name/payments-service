package dexter.banking.commandbus;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Handles the execution of a specific type of command.
 *
 * @param <C> The command type this handler can execute.
 * @param <R> The result type produced by the command.
 */
public interface CommandHandler<C extends Command<R>, R> {

    /**
     * Executes the given command.
     *
     * @param command The command to handle.
     * @return The result of the command execution.
     */
    R handle(C command);

    /**
     * Checks if this handler can handle the specific command instance.
     * <p>
     * This method allows for dynamic, content-based routing. While the command bus
     * first selects candidate handlers based on the command's static {@code Class},
     * this method provides a final check based on the command's internal state.
     * <p>
     * The default implementation returns {@code true}, making this override optional
     * for handlers that do not require content-based inspection.
     *
     * @param command The command instance to inspect.
     * @return {@code true} if this handler should process the command, {@code false} otherwise.
     */
    default boolean matches(C command) {
        return true;
    }

    /**
     * Retrieves the {@code Class} of the command this handler is responsible for.
     * This is used at startup to build an efficient handler lookup map.
     *
     * @return The command's class.
     */
    @SuppressWarnings("unchecked")
    default Class<C> getCommandType() {
        Type[] genericInterfaces = getClass().getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType pt && pt.getRawType().equals(CommandHandler.class)) {
                Type commandType = pt.getActualTypeArguments()[0];
                if (commandType instanceof Class) {
                    return (Class<C>) commandType;
                }
            }
        }
        // Fallback for superclass implementation
        Type genericSuperclass = getClass().getGenericSuperclass();
        if (genericSuperclass instanceof ParameterizedType pt && pt.getRawType().equals(CommandHandler.class)) {
             Type commandType = pt.getActualTypeArguments()[0];
             if (commandType instanceof Class) {
                 return (Class<C>) commandType;
             }
        }
        throw new IllegalStateException("Could not determine command type for handler " + getClass().getName());
    }
}
