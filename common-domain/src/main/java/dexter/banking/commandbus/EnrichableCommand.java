package dexter.banking.commandbus;

import java.util.Map;
import java.util.Optional;

/**
 * An interface for a command that can be enriched with data fragments.
 * The decision to enrich is dictated by the attached journey blueprint.
 */
public interface EnrichableCommand<R> extends JourneyAwareCommand<R> {
    /**
     * Framework-facing method to enrich the command.
     */
    void enrich(Map<Class<? extends EnrichmentFragment>, EnrichmentFragment> fragments);

    /**
     * Public accessor for the handler to retrieve enriched data.
     */
    <F extends EnrichmentFragment> Optional<F> get(Class<F> fragmentType);
}
