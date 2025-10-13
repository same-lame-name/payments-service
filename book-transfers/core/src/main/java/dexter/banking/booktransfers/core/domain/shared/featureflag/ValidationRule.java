package dexter.banking.booktransfers.core.domain.shared.featureflag;

import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;

@FunctionalInterface
public interface ValidationRule {
    boolean isSatisfied(User user, JourneySpecification spec);
}
