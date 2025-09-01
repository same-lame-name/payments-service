package dexter.banking.booktransfers.infrastructure.policy;

import dexter.banking.booktransfers.core.domain.payment.exception.PolicyViolationException;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessAction;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.shared.policy.PolicyEvaluationContext;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * A concrete implementation of the Composite Pattern for BusinessPolicy.
 * It allows multiple policies to be chained together and evaluated as a single unit.
 * Evaluation stops at the first policy that throws a PolicyViolationException.
 */
@RequiredArgsConstructor
public class CompositeBusinessPolicy implements BusinessPolicy {

    private final List<BusinessPolicy> policies;

    @Override
    public void evaluate(PolicyEvaluationContext context, BusinessAction action) throws PolicyViolationException {
        for (BusinessPolicy policy : policies) {
            policy.evaluate(context, action);
        }
    }
}
