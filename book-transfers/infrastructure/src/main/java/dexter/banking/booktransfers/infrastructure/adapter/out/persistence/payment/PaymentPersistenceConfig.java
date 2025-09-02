package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.payment;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
// This annotation specifically tells Spring Data to scan for repository interfaces
// within this package and its sub-packages.
@EnableMongoRepositories(basePackageClasses = PaymentPersistenceConfig.class)
// ComponentScan is still useful for any other package-private @Component beans
// within this adapter, like the mappers and the port implementation.
@ComponentScan(basePackageClasses = PaymentPersistenceConfig.class)

public class PaymentPersistenceConfig {
}
