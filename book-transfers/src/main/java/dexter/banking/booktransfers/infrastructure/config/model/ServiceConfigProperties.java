package dexter.banking.booktransfers.infrastructure.config.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotNull;
import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "service-config")
@Data
@Validated
public class ServiceConfigProperties {
    @NotNull
    private Map<String, CommandConfig> commands;
}
