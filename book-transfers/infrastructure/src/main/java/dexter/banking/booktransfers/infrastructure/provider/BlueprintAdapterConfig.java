package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.aspectj.EnableSpringConfigured;

@Configuration
@EnableSpringConfigured
@ComponentScan(basePackageClasses = BlueprintAdapterConfig.class)
public class BlueprintAdapterConfig implements FacadeConfiguration {
}
