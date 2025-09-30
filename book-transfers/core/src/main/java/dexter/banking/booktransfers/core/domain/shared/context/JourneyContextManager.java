package dexter.banking.booktransfers.core.domain.shared.context;

import java.util.function.Supplier;

/**
 * Manages the lifecycle of the JourneyContext using ScopedValues, providing a clean,
 * application-level API for running operations within a specific journey's scope.
 */
public final class JourneyContextManager {
    private static final ScopedValue<JourneyContext> CONTEXT = ScopedValue.newInstance();

    private JourneyContextManager() {}

    public static JourneyContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Executes a standard Supplier within a context scope.
     * @param context The JourneyContext to set for the operation.
     * @param operation The operation to execute.
     * @return The result of the operation.
     */
    public static <R> R runWithContext(JourneyContext context, Supplier<R> operation) throws Exception {
        return ScopedValue.where(CONTEXT, context).call(operation::get);
    }
}
