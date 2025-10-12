package dexter.banking.booktransfers.core.application.featureflag;

import dexter.banking.booktransfers.core.domain.featureflag.UserGroup;
import dexter.banking.booktransfers.core.domain.featureflag.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ValidationRuleRegistry {
    private final Map<UserGroup, ValidationRule> registry;
    private final ValidationRule defaultRule = (user, spec) -> user.groups().stream()
            .anyMatch(spec.getFeatureFlag().pilotGroups()::contains);

    public ValidationRuleRegistry(Set<ValidationRule> rules) {
        this.registry = rules.stream()
                .collect(Collectors.toMap(this::extractUserGroup, Function.identity()));
    }

    public ValidationRule getRuleFor(UserGroup group) {
        return registry.getOrDefault(group, defaultRule);
    }

    // A placeholder method to associate a rule with a group.
    // In a real implementation, this might use annotations or a naming convention.
    private UserGroup extractUserGroup(ValidationRule rule) {
        // This is a simplified stand-in. A real implementation would need a robust
        // way to link a rule instance to its UserGroup enum.
        // For now, we'll assume a naming convention like "InternalAuditorsValidationRule".
        String simpleName = rule.getClass().getSimpleName();
        String enumName = simpleName.replace("ValidationRule", "").toUpperCase();
        try {
            return UserGroup.valueOf(enumName);
        } catch (IllegalArgumentException e) {
            // This will need a more robust strategy.
            // For this MVP, we will rely on a strict naming convention.
            // A better approach might be a custom annotation on the rule implementation.
            throw new IllegalStateException("Could not derive UserGroup from rule class name: " + simpleName);
        }
    }
}
