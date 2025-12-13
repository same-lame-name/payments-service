package dexter.banking.limit.config;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RsqlFilterConfig {

    @Bean
    public FilterConfig<String> payeeJpaFilterConfig() {
        return new FilterConfig<String>()
                .with("name", "name")
                .with("iban", "iban")
                .with("id", "id");
    }
}