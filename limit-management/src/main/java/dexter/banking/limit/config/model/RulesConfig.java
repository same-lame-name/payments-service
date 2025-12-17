package dexter.banking.limit.config.model;

public record RulesConfig(
    boolean sanctionsCheckRequired,
    boolean highRiskCheckRequired
) {}