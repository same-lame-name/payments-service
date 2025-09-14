package dexter.banking.booktransfers.infrastructure.context;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * The public, atomic facade that enables and exposes the AOP infrastructure.
 * Placing @EnableAspectJAutoProxy here ensures that the AOP system is active
 * if and only if this facade is active.
 */
@Configuration
@EnableAspectJAutoProxy
@ComponentScan(basePackageClasses = AspectFacadeConfiguration.class)
public class AspectFacadeConfiguration implements FacadeConfiguration {

}
