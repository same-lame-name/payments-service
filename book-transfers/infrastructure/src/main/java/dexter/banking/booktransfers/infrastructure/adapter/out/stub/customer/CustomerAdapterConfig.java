package dexter.banking.booktransfers.infrastructure.adapter.out.stub.customer;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = CustomerAdapterConfig.class)
public class CustomerAdapterConfig implements FacadeConfiguration {
}
