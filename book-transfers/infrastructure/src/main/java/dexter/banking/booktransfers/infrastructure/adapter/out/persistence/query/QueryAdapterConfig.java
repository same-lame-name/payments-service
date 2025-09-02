package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.query;

import dexter.banking.booktransfers.infrastructure.adapter.out.persistence.idempotency.IdempotencyPersistenceConfig;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackageClasses = QueryAdapterConfig.class)
@ComponentScan(basePackageClasses = QueryAdapterConfig.class)
public class QueryAdapterConfig {
}
