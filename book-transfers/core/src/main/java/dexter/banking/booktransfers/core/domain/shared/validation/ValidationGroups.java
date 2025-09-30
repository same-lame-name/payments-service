package dexter.banking.booktransfers.core.domain.shared.validation;

/**
 * A namespace interface for all JSR 303 validation groups.
 * This provides a single, discoverable location for all rule sets.
 */
public interface ValidationGroups {
    /**
     * Validation rules applicable to a standard payment submission.
     */
    interface StandardPayment {}

    /**
     * Validation rules applicable to a wallet top-up operation.
     */
    interface WalletJourney {}
}
