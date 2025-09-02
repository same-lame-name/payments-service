package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;

import java.util.Optional;
/**
 * A Driven Port defining the contract for a technology-agnostic configuration provider.
 * The core uses this port to request the configuration it needs, without knowing where
 * the configuration comes from (e.g., properties file, config server).
 */
public interface ConfigurationPort {
    /**
     * Finds the journey-specific configuration.
     * @param journeyIdentifier The unique identifier of the journey (e.g., "PAYMENT_SUBMIT_V1").
     * @return An Optional containing the pure, domain-aligned configuration if found.
     */
    Optional<JourneySpecification> findForJourney(String journeyIdentifier);
}
