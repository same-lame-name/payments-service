package dexter.banking.limit.config.model;

import java.util.Set;

public record RulesConfig(
    boolean sanctionsCheckRequired,
    boolean highRiskCheckRequired,
    Set<String> editableFields

) {
}