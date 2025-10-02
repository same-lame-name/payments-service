package dexter.banking.booktransfers.core.domain.payment.valueobject;

import dexter.banking.commandbus.EnrichmentFragment;

import java.math.BigDecimal;

/**
 * Example enrichment fragment for account balance data.
 * (Temporary location)
 */
public record AccountBalanceFragment(String accountNumber, BigDecimal balance) implements EnrichmentFragment {
}
