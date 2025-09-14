package dexter.banking.booktransfers.core.domain.shared.context;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as the beginning of a new business journey, establishing a fresh
 * JourneyContext for the operation's scope. The value is a SpEL expression used
 * to extract the journey name from the method's arguments.
 *
 * Example: @BeginJourney("#journeyName") or @BeginJourney("#event.getJourneyIdentifier()")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BeginJourney {
    String value();
}
