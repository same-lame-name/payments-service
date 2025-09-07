package dexter.banking.booktransfers.infrastructure.adapter.out.config;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = ConfigAdapterConfig.class)
public class ConfigAdapterConfig implements FacadeConfiguration {
}
