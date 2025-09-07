package dexter.banking.booktransfers.core.port.out;


import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;

/**
 * A driven port defining the contract for an adapter that can construct
 * the correct, fully-composed BusinessPolicy from a journey's specification.
 * This contract is explicit: it requires the full specification to create the policy.
 */
public interface BusinessPolicyFactory {
    /**
     * Creates a composed policy from a specific journey specification.
     *
     * @param spec The complete, unified configuration for the journey.
     * @return The composed BusinessPolicy.
     * @throws IllegalArgumentException if any policy bean name in the spec cannot be resolved.
     */
    BusinessPolicy create(JourneySpecification spec);
}
