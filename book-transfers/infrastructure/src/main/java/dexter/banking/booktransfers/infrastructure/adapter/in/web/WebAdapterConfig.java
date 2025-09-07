package dexter.banking.booktransfers.infrastructure.adapter.in.web;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = WebAdapterConfig.class)
public class WebAdapterConfig implements FacadeConfiguration {
}
