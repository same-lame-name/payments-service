package dexter.banking.booktransfers.infrastructure.adapter.in.cli;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = CliAdapterConfig.class)
public class CliAdapterConfig implements FacadeConfiguration {
}
