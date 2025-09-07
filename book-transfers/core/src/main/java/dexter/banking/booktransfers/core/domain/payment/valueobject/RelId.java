package dexter.banking.booktransfers.core.domain.payment.valueobject;

import dexter.banking.booktransfers.core.domain.shared.primitives.ValueObject;

import java.util.UUID;

public record RelId(UUID value) implements ValueObject {
}
