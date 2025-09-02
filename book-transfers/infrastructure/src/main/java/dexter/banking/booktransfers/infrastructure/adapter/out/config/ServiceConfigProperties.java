package dexter.banking.booktransfers.infrastructure.adapter.out.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import java.util.Map;
@Configuration
@ConfigurationProperties(prefix = "service-config")
@Data
@Validated
class ServiceConfigProperties {
    @NotNull
    private Map<String, JourneyProperties> journeys;
}
