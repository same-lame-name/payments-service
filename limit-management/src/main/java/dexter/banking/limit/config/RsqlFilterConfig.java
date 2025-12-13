package dexter.banking.limit.config;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class RsqlFilterConfig {

    @Bean
    public FilterConfig<Function<Payee, ?>> payeeInMemoryFilterConfig() {
        return new FilterConfig<Function<Payee, ?>>()
                .with("name", Payee::getName)
                .with("iban", Payee::getIban)
                .with("id", Payee::getId);
    }

    @Bean
    public FilterConfig<String> payeeJpaFilterConfig() {
        return new FilterConfig<String>()
                .with("name", "name")
                .with("iban", "iban")
                .with("id", "id");
    }
}