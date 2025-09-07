package dexter.banking.booktransfers.core.domain.payment.event;

import dexter.banking.booktransfers.core.domain.payment.PaymentState;
import dexter.banking.booktransfers.core.domain.shared.primitives.DomainEvent;

import java.util.UUID;

/**
 * A specialized event contract for events raised by the Payment aggregate.
 * It extends the generic DomainEvent to include the specific PaymentState.
 */
public interface PaymentEvent extends DomainEvent<UUID> {
    /** @return The state of the Payment aggregate at the time the event was raised. */
    PaymentState aggregateState();
}
