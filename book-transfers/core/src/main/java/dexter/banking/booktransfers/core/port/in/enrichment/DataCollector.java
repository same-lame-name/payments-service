package dexter.banking.booktransfers.core.port.in.enrichment;

import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.EnrichmentFragment;

/**
 * A pure function responsible for collecting a specific piece of external data
 * related to a command, returning it as an immutable EnrichmentFragment.
 *
 * @param <C> The type of command this collector operates on.
 * @param <F> The type of fragment this collector produces.
 */
@FunctionalInterface
public interface DataCollector<C extends Command<?>, F extends EnrichmentFragment> {
    F collect(C command);
}
