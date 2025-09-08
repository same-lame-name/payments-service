package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.payment.filesystem;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("filesystem")
@ComponentScan(basePackageClasses = FileSystemPersistenceAdapterConfig.class)
public class FileSystemPersistenceAdapterConfig implements FacadeConfiguration {
}
