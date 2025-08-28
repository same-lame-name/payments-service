package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.config.CommandConfiguration;

import java.util.Optional;

/**
 * A Driven Port defining the contract for a technology-agnostic configuration provider.
 * The core uses this port to request the configuration it needs, without knowing where
 * the configuration comes from (e.g., properties file, config server).
 */
public interface ConfigurationPort {
    /**
     * Finds the command-specific configuration.
     * @param commandIdentifier The unique identifier of the command (e.g., "PAYMENT_SUBMIT_V1").
     * @return An Optional containing the pure, domain-aligned configuration if found.
     */
    Optional<CommandConfiguration> findForCommand(String commandIdentifier);
}
