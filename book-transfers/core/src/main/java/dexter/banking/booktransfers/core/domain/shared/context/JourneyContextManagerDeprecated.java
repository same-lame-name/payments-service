package dexter.banking.booktransfers.core.domain.shared.context;

import java.lang.ScopedValue;
import java.util.concurrent.Callable;

/**
 * Manages the lifecycle of the JourneyContext, providing a clean, application-level
 * API for running operations within a specific journey's scope.
 */
public final class JourneyContextManagerDeprecated {

    static final ScopedValue<JourneyContextDeprecated> CONTEXT = ScopedValue.newInstance();

    private JourneyContextManagerDeprecated() { /* Private constructor */ }

    public static JourneyContextDeprecated getContext() {
        return CONTEXT.get();
    }

    /**
     * Executes a standard Callable within a context scope.
     */
    public static <T> T runWithContext(JourneyContextDeprecated context, Callable<T> operation) throws Exception {
        return ScopedValue.where(CONTEXT, context).call(operation);
    }
}
