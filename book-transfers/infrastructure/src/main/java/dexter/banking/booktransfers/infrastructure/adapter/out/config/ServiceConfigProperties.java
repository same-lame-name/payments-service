package dexter.banking.booktransfers.infrastructure.adapter.out.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.HashMap;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app")
public class ServiceConfigProperties {
    private Map<String, JourneyProperties> journeys = new HashMap<>();
}
