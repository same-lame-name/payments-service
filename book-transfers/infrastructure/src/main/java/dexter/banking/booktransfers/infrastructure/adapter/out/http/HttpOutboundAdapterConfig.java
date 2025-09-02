package dexter.banking.booktransfers.infrastructure.adapter.out.http;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = HttpOutboundAdapterConfig.class)
public class HttpOutboundAdapterConfig {
}
