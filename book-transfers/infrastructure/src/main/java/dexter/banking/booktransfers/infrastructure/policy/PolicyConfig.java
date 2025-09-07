package dexter.banking.booktransfers.infrastructure.policy;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = PolicyConfig.class)
public class PolicyConfig implements FacadeConfiguration {
}
