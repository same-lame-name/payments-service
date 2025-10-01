package dexter.banking.booktransfers.core.domain.shared.blueprint.spec;

import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.validation.ValidationGroup;

import java.util.List;

/**
 * A base interface for all optional journey blueprint contracts.
 * <p>
 * All the methods in this interface should have default implementations, since they are optional in nature.
 * These values can be left out in the properties.
 * Properties can be mentioned explicitly in case overriding from default behaviour is needed.
 */
public interface BaseJourneyBlueprint extends JourneyBlueprint {
    // === COMMON & MANDATORY ===
    List<ValidationGroup> getValidationGroups();
    List<String> getDataCollectors();
    List<String> getBusinessRules();

    // === COMMON & OPTIONAL ===
    boolean isIdempotencyEnabled();
}
