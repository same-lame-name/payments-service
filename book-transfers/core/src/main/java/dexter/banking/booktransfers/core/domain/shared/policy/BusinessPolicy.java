package dexter.banking.booktransfers.core.domain.shared.policy;


import dexter.banking.booktransfers.core.domain.payment.exception.PolicyViolationException;

/**
 * The generic, universal interface for all business rule evaluation.
 * This is a domain-level port, allowing infrastructure-level policies
 * to be plugged into the domain model.
 */
@FunctionalInterface
public interface BusinessPolicy {
    /**
     * Evaluates the policy against a given context and action.
     *
     * @param context The immutable context of the aggregate's state.
     * @param action The business action being attempted.
     * @throws PolicyViolationException if any rule is violated.
     */
    void evaluate(PolicyEvaluationContext context, BusinessAction action) throws PolicyViolationException;
}
