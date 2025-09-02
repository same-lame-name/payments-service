package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.compliance.mongo;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackageClasses = CompliancePersistenceConfig.class)
@ComponentScan(basePackageClasses = CompliancePersistenceConfig.class)
public class CompliancePersistenceConfig {
}
