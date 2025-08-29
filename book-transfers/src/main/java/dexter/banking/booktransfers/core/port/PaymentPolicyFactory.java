package dexter.banking.booktransfers.core.port;

import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;

/**
 * A driven port defining the contract for an adapter that can construct
 * the correct, fully-composed BusinessPolicy for a given business context (journey).
 */
public interface PaymentPolicyFactory {
    /**
     * Gets the composed policy for a specific journey.
     *
     * @param journeyName The unique identifier for the journey (e.g., "V1_PROCEDURAL_PAYMENT").
     * @return The composed BusinessPolicy.
     * @throws IllegalArgumentException if no policy is configured for the given journey name.
     */
    BusinessPolicy getPolicyForJourney(String journeyName);
}
