package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.payment.mongo;

import dexter.banking.booktransfers.infrastructure.FacadeConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackageClasses = PaymentPersistenceConfig.class)
@ComponentScan(basePackageClasses = PaymentPersistenceConfig.class)
public class PaymentPersistenceConfig implements FacadeConfiguration {
}
