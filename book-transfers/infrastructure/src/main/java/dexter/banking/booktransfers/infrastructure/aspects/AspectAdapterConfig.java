package dexter.banking.booktransfers.infrastructure.aspects;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;

@Configuration
@EnableSpringConfigured
@ComponentScan(basePackageClasses = AspectAdapterConfig.class)
public class AspectAdapterConfig implements FacadeConfiguration {
}
