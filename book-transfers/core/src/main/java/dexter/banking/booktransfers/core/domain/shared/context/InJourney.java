package dexter.banking.booktransfers.core.domain.shared.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a parameter in a method to be injected with the JourneyBlueprint
 * from the current JourneyContext.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface InJourney {
}
