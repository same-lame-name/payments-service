package dexter.banking.booktransfers.infrastructure.policy;

import dexter.banking.booktransfers.core.domain.payment.PaymentState;
import dexter.banking.booktransfers.core.domain.payment.exception.PolicyViolationException;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessAction;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.shared.policy.PolicyEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Collections;

/**
 * A concrete BusinessPolicy that enforces the state transition graph for a Payment.
 * It uses a declarative, fluent DSL to construct the state machine rules,
 * making them human-readable, composable, and easier to maintain.
 */
@Component("stateTransitionPolicy")
public class StateTransitionPolicy implements BusinessPolicy {

    private static final Map<BusinessAction, Set<PaymentState>> ALLOWED_TRANSITIONS = new TransitionRuleBuilder()
        .allow(BusinessAction.START_PAYMENT).from(PaymentState.NEW)

        // Limit Earmark
        .allow(BusinessAction.RECORD_LIMIT_EARMARK_SUCCESS).from(PaymentState.NEW, PaymentState.PENDING_COMPLIANCE)
        .allow(BusinessAction.RECORD_LIMIT_EARMARK_FAILURE).from(PaymentState.NEW, PaymentState.PENDING_COMPLIANCE)

        // Debit
        .allow(BusinessAction.RECORD_DEBIT_SUCCESS).from(PaymentState.LIMIT_RESERVED)
        .allow(BusinessAction.RECORD_DEBIT_FAILURE).from(PaymentState.LIMIT_RESERVED)

        // Credit
        .allow(BusinessAction.RECORD_CREDIT_SUCCESS).from(PaymentState.FUNDS_DEBITED)
        .allow(BusinessAction.RECORD_CREDIT_FAILURE).from(PaymentState.FUNDS_DEBITED)

        // Debit Reversal (from Credit Failure)
        .allow(BusinessAction.RECORD_DEBIT_REVERSAL_SUCCESS).from(PaymentState.FUNDS_COULD_NOT_BE_CREDITED)
        .allow(BusinessAction.RECORD_DEBIT_REVERSAL_FAILURE).from(PaymentState.FUNDS_COULD_NOT_BE_CREDITED)

        // Limit Reversal (from Debit Failure or Debit Reversal)
        .allow(BusinessAction.RECORD_LIMIT_REVERSAL_SUCCESS).from(PaymentState.FUNDS_COULD_NOT_BE_DEBITED, PaymentState.FUNDS_DEBIT_REVERSED)
        .allow(BusinessAction.RECORD_LIMIT_REVERSAL_FAILURE).from(PaymentState.FUNDS_COULD_NOT_BE_DEBITED, PaymentState.FUNDS_DEBIT_REVERSED)

        // Finalization
        .allow(BusinessAction.RECORD_PAYMENT_SETTLED).from(PaymentState.FUNDS_CREDITED)
        .allow(BusinessAction.RECORD_PAYMENT_FAILED).from(PaymentState.LIMIT_REVERSED, PaymentState.LIMIT_COULD_NOT_BE_RESERVED)

        .allow(BusinessAction.RECORD_REMEDIATION_NEEDED).from(EnumSet.allOf(PaymentState.class))

        .build();


    @Override
    public void evaluate(PolicyEvaluationContext context, BusinessAction action) throws PolicyViolationException {
        final PaymentState currentState = context.paymentMemento().state();
        final Set<PaymentState> allowedStates = ALLOWED_TRANSITIONS.get(action);

        if (allowedStates == null || !allowedStates.contains(currentState)) {
            throw new PolicyViolationException(
                    "Action " + action + " is not allowed in the current state " + currentState +
                            ". Allowed states are: " + (allowedStates != null ? allowedStates : "NONE")
            );
        }
    }

    /**
     * A private fluent builder to create a readable DSL for defining transition rules.
     */
    private static class TransitionRuleBuilder {
        private final Map<BusinessAction, Set<PaymentState>> rules = new HashMap<>();
        private BusinessAction currentAction;

        public ActionConfigurer allow(BusinessAction action) {
            this.currentAction = action;
            return new ActionConfigurer(this);
        }

        private void addRule(Set<PaymentState> states) {
            rules.put(currentAction, Collections.unmodifiableSet(states));
        }

        public Map<BusinessAction, Set<PaymentState>> build() {
            return Collections.unmodifiableMap(rules);
        }

        // Inner class to enforce the .from() part of the DSL
        private static class ActionConfigurer {
            private final TransitionRuleBuilder parentBuilder;

            ActionConfigurer(TransitionRuleBuilder parentBuilder) {
                this.parentBuilder = parentBuilder;
            }

            public TransitionRuleBuilder from(PaymentState... allowedStates) {
                parentBuilder.addRule(EnumSet.of(allowedStates[0], allowedStates));
                return parentBuilder;
            }

            public TransitionRuleBuilder from(Set<PaymentState> allowedStates) {
                parentBuilder.addRule(allowedStates);
                return parentBuilder;
            }
        }
    }
}