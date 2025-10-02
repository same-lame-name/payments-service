package dexter.banking.commandbus;

import java.util.Map;
import java.util.Optional;

/**
 * Abstract base class providing common implementation for EnrichableCommand.
 */
public abstract class AbstractEnrichableCommand<R> extends AbstractJourneyAwareCommand<R> implements EnrichableCommand<R> {

    protected Map<Class<? extends EnrichmentFragment>, EnrichmentFragment> fragments;

    @Override
    public void enrich(Map<Class<? extends EnrichmentFragment>, EnrichmentFragment> fragments) {
        if (this.fragments != null) {
            throw new IllegalStateException("Command has already been enriched.");
        }
        this.fragments = Map.copyOf(fragments);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <F extends EnrichmentFragment> Optional<F> get(Class<F> fragmentType) {
        return (this.fragments == null)
                ? Optional.empty()
                : Optional.ofNullable((F) this.fragments.get(fragmentType));
    }
}
