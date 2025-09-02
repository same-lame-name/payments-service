package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.idempotency.mongo;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackageClasses = IdempotencyPersistenceConfig.class)
@ComponentScan(basePackageClasses = IdempotencyPersistenceConfig.class)
public class IdempotencyPersistenceConfig {
}
