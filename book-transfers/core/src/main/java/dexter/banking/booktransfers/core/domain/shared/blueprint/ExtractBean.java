package dexter.banking.booktransfers.core.domain.shared.blueprint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guarantees that the configured String (or List of Strings) corresponds to a valid
 * Spring bean and returns the actual bean instance(s) from the ApplicationContext.
 * <p>
 * This annotation performs both VALIDATION and EXTRACTION.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ExtractBean {
}
