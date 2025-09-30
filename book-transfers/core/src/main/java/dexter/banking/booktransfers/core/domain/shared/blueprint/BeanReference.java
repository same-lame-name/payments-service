package dexter.banking.booktransfers.core.domain.shared.blueprint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Explicitly marks a method on a JourneyBlueprint interface as returning a
 * reference to a Spring Bean. The string value in the corresponding YAML
 * configuration will be interpreted as a bean name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface BeanReference {
}
