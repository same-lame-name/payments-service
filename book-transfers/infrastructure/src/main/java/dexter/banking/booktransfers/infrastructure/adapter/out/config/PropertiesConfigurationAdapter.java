package dexter.banking.booktransfers.infrastructure.adapter.out.config;

import dexter.banking.booktransfers.core.domain.shared.config.JourneySpecification;
import dexter.banking.booktransfers.core.port.out.ConfigurationPort;
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
    public Optional<JourneySpecification> findForJourney(String journeyIdentifier) {
        // 1. Get the infrastructure-specific configuration object.
        JourneyProperties infraConfig = serviceConfigProperties.getJourneys().get(journeyIdentifier);

        // 2. Map it to the pure core/domain configuration object.
        return Optional.ofNullable(infraConfig).map(this::toDomain);
    }

    private JourneySpecification toDomain(JourneyProperties infraConfig) {
        return new JourneySpecification(infraConfig.isIdempotencyEnabled(), infraConfig.getPolicies());
    }
}
