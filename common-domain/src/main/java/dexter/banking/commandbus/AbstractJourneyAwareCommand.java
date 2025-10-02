package dexter.banking.commandbus;

/**
 * Abstract base class providing common implementation for JourneyAwareCommand.
 */
public abstract class AbstractJourneyAwareCommand<R> implements JourneyAwareCommand<R> {

    protected Blueprint blueprint;

    @Override
    public void setBlueprint(Blueprint blueprint) {
        if (this.blueprint != null) {
            throw new IllegalStateException("Blueprint has already been set.");
        }
        this.blueprint = blueprint;
    }

    @Override
    public <T extends Blueprint> T getBlueprint(Class<T> blueprintType) {
        if (this.blueprint == null) {
            throw new IllegalStateException("Blueprint not set on command.");
        }
        return blueprintType.cast(this.blueprint);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Blueprint> T getBlueprint() {
        if (this.blueprint == null) {
            throw new IllegalStateException("Blueprint not set on command.");
        }
        return (T) this.blueprint;
    }
}
