package dexter.banking.booktransfers.infrastructure.policy;

import dexter.banking.booktransfers.core.domain.exception.PolicyViolationException;
import dexter.banking.booktransfers.core.domain.model.PaymentState;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessAction;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.model.policy.PolicyEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * A concrete BusinessPolicy that enforces the fundamental state transition graph for a Payment.
 * It ensures that a business action is only valid if the aggregate is in an appropriate pre-condition state.
 */
@Component("stateTransitionPolicy")
public class StateTransitionPolicy implements BusinessPolicy {

    @Override
    public void evaluate(PolicyEvaluationContext context, BusinessAction action) throws PolicyViolationException {
        PaymentState currentState = context.paymentMemento().state();
        Set<PaymentState> allowedStates = getAllowedStatesFor(action);

        if (!allowedStates.contains(currentState)) {
            throw new PolicyViolationException(
                    "Action " + action + " is not allowed in the current state " + currentState +
                            ". Allowed states are: " + allowedStates
            );
        }
    }

    private Set<PaymentState> getAllowedStatesFor(BusinessAction action) {
        return switch (action) {
            case START_PAYMENT -> Set.of(PaymentState.NEW);
            case RECORD_LIMIT_EARMARK_SUCCESS, RECORD_LIMIT_EARMARK_FAILURE -> Set.of(PaymentState.NEW, PaymentState.PENDING_COMPLIANCE);
            case RECORD_DEBIT_SUCCESS, RECORD_DEBIT_FAILURE -> Set.of(PaymentState.LIMIT_RESERVED);
            case RECORD_CREDIT_SUCCESS, RECORD_CREDIT_FAILURE -> Set.of(PaymentState.FUNDS_DEBITED);
            case RECORD_DEBIT_REVERSAL_SUCCESS, RECORD_DEBIT_REVERSAL_FAILURE -> Set.of(PaymentState.FUNDS_DEBITED); // From credit failure
            case RECORD_LIMIT_REVERSAL_SUCCESS, RECORD_LIMIT_REVERSAL_FAILURE -> Set.of(PaymentState.LIMIT_RESERVED); // From debit failure
        };
    }
}
