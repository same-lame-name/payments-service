package dexter.banking.booktransfers.infrastructure.adapter.out.config;

import dexter.banking.booktransfers.core.domain.model.config.CommandConfiguration;
import dexter.banking.booktransfers.core.port.ConfigurationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * The concrete Driven Adapter that implements the ConfigurationPort.
 * This adapter knows about the specific way configuration is loaded in this application
 * (via Spring's @ConfigurationProperties) and acts as an Anti-Corruption Layer by
 * mapping the infrastructure-specific model to the pure core model.
 */
@Component
@RequiredArgsConstructor
public class PropertiesConfigurationAdapter implements ConfigurationPort {

    private final ServiceConfigProperties serviceConfigProperties;

    @Override
    public Optional<CommandConfiguration> findForCommand(String commandIdentifier) {
        // 1. Get the infrastructure-specific configuration object.
        CommandConfig infraConfig = serviceConfigProperties.getCommands().get(commandIdentifier);

        // 2. Map it to the pure core/domain configuration object.
        return Optional.ofNullable(infraConfig).map(this::toDomain);
    }

    private CommandConfiguration toDomain(CommandConfig infraConfig) {
        return new CommandConfiguration(infraConfig.isIdempotencyApplicable());
    }
}
