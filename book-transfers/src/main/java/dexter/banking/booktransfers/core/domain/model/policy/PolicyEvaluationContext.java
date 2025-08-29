package dexter.banking.booktransfers.core.domain.model.policy;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.primitives.ValueObject;

import java.util.Map;

/**
 * An immutable record used to pass all necessary information to a BusinessPolicy for evaluation.
 *
 * @param paymentMemento The snapshot of the aggregate's current state.
 * @param metadata       A flexible map for passing additional, non-domain data (e.g., from a command).
 */
public record PolicyEvaluationContext(
    Payment.PaymentMemento paymentMemento,
    Map<String, Object> metadata
) implements ValueObject {
}
