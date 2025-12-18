package dexter.banking.limit.config.model;

public record ServiceConfig(
    boolean ibanValidationEnabled,
    boolean bankEnrichmentEnabled,
    String validationRegex,
    boolean idempotencyEnabled
) {}