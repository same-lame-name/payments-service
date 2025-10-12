package dexter.banking.booktransfers.infrastructure.adapter.out.featureflag;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackageClasses = FeatureFlagAdapterFacade.class)
public class FeatureFlagAdapterFacade implements FacadeConfiguration {
}
