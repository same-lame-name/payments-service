package dexter.banking.commandbus;

/**
 * The foundational interface for any command that is aware of its journey blueprint.
 */
public interface JourneyAwareCommand<R> extends Command<R> {
    /**
     * Framework-facing method to attach the journey blueprint.
     */
    void setBlueprint(Blueprint blueprint);

    /**
     * Retrieves the blueprint and casts it to the requested type.
     * @throws ClassCastException if the blueprint is not of the requested type.
     */
    <T extends Blueprint> T getBlueprint(Class<T> blueprintType);

    /**
     * Retrieves the blueprint, inferring the type from the assignment context.
     * @throws ClassCastException if the blueprint cannot be cast to the inferred type.
     */
    <T extends Blueprint> T getBlueprint();
}
