package dexter.banking.limit.config;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import dexter.banking.limit.repository.rsql.common.SortConfig;
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
                .with("id", Payee::getId)
                .with("city", payee -> payee.getAddress().getCity()); // Nested for in-memory
    }

    @Bean
    public FilterConfig<String> payeeJpaFilterConfig() {
        return new FilterConfig<String>()
                .with("name", "name")
                .with("iban", "iban")
                .with("id", "id")
                .with("city", "address.city") // Map flat 'city' to nested 'address.city'
                .with("zip", "address.zip");
    }

    @Bean
    public SortConfig<String> payeeJpaSortConfig() {
        return new SortConfig<String>()
                .with("name", "name")
                .with("iban", "iban")
                .with("id", "id")
                .with("city", "address.city"); // Allow sorting by city
    }

    @Bean
    public SortConfig<Function<Payee, ? extends Comparable>> payeeInMemorySortConfig() {
        return new SortConfig<Function<Payee, ? extends Comparable>>()
                .with("name", Payee::getName)
                .with("iban", Payee::getIban)
                .with("id", Payee::getId)
                .with("city", payee -> payee.getAddress().getCity());
    }
}