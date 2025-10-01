package dexter.banking.booktransfers.core.domain.shared.blueprint;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Guarantees that the configured String (or List of Strings) corresponds to a valid
 * Spring bean name in the ApplicationContext.
 * <p>
 * This annotation performs VALIDATION ONLY. The proxy will still return the original
 * String literal(s) from the configuration, not the bean instance(s).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface VerifyBean {
}
