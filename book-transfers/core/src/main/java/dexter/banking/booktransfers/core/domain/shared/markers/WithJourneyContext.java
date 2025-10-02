package dexter.banking.booktransfers.core.domain.shared.markers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Establishes a JourneyContext for the duration of the annotated method.
 * This is used for ad-hoc processes that are not initiated by a standard Command.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WithJourneyContext {
    /**
     * A SpEL expression to extract the journey identifier string from the method arguments.
     * Example: "#request.journeyName"
     */
    String journeyIdentifier();
}
