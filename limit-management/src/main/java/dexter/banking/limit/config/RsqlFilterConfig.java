package dexter.banking.limit.config;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.repository.rsql.FilterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RsqlFilterConfig {

    @Bean
    public FilterConfig<Payee> payeeFilterConfig() {
        return new FilterConfig<Payee>()
                .with("name", Payee::getName)
                .with("iban", Payee::getIban)
                .with("id", Payee::getId);
    }
}