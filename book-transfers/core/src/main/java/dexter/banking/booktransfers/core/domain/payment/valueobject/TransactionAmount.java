package dexter.banking.booktransfers.core.domain.payment.valueobject;

import dexter.banking.booktransfers.core.domain.shared.primitives.ValueObject;

import java.math.BigDecimal;
import java.util.Currency;

public record TransactionAmount(
        BigDecimal amount,
        Currency currency
) implements ValueObject {
}
