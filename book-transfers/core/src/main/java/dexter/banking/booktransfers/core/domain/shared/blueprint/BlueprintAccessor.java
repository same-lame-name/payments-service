package dexter.banking.booktransfers.core.domain.shared.blueprint;

/**
 * A stateless service that provides safe, typed access to the JourneyBlueprint
 * associated with the current execution context.
 */
public interface BlueprintAccessor {

    /**
     * Retrieves the current journey's blueprint, inferring the type from the
     * assignment context. This is the preferred method for accessing the blueprint.
     *
     * @param <T> The specific blueprint type to be inferred by the compiler.
     * @return The fully-typed blueprint for the current journey.
     * @throws IllegalStateException if no journey context is active.
     * @throws ClassCastException if the retrieved blueprint cannot be cast to the inferred type.
     */
    <T extends JourneyBlueprint> T get();

    /**
     * Retrieves the current journey's blueprint and safely casts it to the requested type.
     *
     * @param blueprintType The specific {@link JourneyBlueprint} interface class desired.
     * @param <T>           The specific blueprint type.
     * @return The blueprint for the current journey.
     * @throws IllegalStateException if no journey context is active.
     * @throws ClassCastException    if the current journey's blueprint is not an instance
     *                               of the requested blueprintType.
     */
    <T extends JourneyBlueprint> T get(Class<T> blueprintType);
}

