package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = MessagingInboundAdapterConfig.class)
public class MessagingInboundAdapterConfig {
}
