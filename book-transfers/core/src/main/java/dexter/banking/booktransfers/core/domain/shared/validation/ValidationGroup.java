package dexter.banking.booktransfers.core.domain.shared.validation;

public enum ValidationGroup {
    STANDARD_PAYMENT(ValidationGroups.StandardPayment.class),
    WALLET_JOURNEY(ValidationGroups.WalletJourney.class);

    private final Class<?> groupClass;

    ValidationGroup(Class<?> groupClass) { this.groupClass = groupClass; }

    public Class<?> getGroupClass() { return groupClass; }
}
