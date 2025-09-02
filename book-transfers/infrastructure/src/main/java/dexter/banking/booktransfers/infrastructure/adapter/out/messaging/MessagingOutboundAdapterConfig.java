package dexter.banking.booktransfers.infrastructure.adapter.out.messaging;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = MessagingOutboundAdapterConfig.class)
public class MessagingOutboundAdapterConfig {
}
