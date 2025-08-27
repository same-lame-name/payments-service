package dexter.banking.booktransfers.core.middleware.context;

import org.springframework.core.NamedThreadLocal;

import java.util.Optional;

/**
 * Manages the CommandProcessingContext for the current thread using a ThreadLocal.
 * This is a standard pattern for handling request-scoped data in a clean way.
 */
public final class CommandProcessingContextHolder {

    private static final ThreadLocal<CommandProcessingContext> contextHolder =
            new NamedThreadLocal<>("Command Processing Context");
    private CommandProcessingContextHolder() {}

    public static void clearContext() {
        contextHolder.remove();
    }

    public static void setContext(CommandProcessingContext context) {
        if (context == null) {
            clearContext();
        } else {
            contextHolder.set(context);
        }
    }

    public static Optional<CommandProcessingContext> getContext() {
        return Optional.ofNullable(contextHolder.get());
    }
}
