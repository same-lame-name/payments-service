package dexter.banking.booktransfers.core.domain.shared.context;

import java.lang.ScopedValue;
import java.util.concurrent.Callable;

/**
 * Manages the lifecycle of the JourneyContext, providing a clean, application-level
 * API for running operations within a specific journey's scope.
 */
public final class JourneyContextManager {

    static final ScopedValue<JourneyContext> CONTEXT = ScopedValue.newInstance();

    private JourneyContextManager() { /* Private constructor */ }

    public static JourneyContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Executes a standard Callable within a context scope.
     */
    public static <T> T runWithContext(JourneyContext context, Callable<T> operation) throws Exception {
        return ScopedValue.where(CONTEXT, context).call(operation);
    }
}
