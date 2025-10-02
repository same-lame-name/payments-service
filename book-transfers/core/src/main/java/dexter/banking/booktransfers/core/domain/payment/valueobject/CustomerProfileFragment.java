package dexter.banking.booktransfers.core.domain.payment.valueobject;

import dexter.banking.commandbus.EnrichmentFragment;

/**
 * Example enrichment fragment for customer profile data.
 * (Temporary location)
 */
public record CustomerProfileFragment(String customerId, String name, String email) implements EnrichmentFragment {
}
