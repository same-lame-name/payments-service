package dexter.banking.booktransfers.infrastructure.adapter.out.events;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = EventsAdapterConfig.class)
public class EventsAdapterConfig implements FacadeConfiguration {
}
