package dexter.banking.booktransfers.infrastructure.policy;

import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.PaymentPolicyFactory;
import dexter.banking.booktransfers.infrastructure.config.JourneyConfig;
import dexter.banking.booktransfers.infrastructure.config.ServiceConfigProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The concrete Driven Adapter for the PaymentPolicyFactory.
 * This adapter reads journey configurations from YAML properties, retrieves the
 * corresponding policy beans from the Spring ApplicationContext, and assembles them
 * into a CompositeBusinessPolicy.
 */
@Component
@RequiredArgsConstructor
public class YamlPaymentPolicyFactory implements PaymentPolicyFactory {

    private final ServiceConfigProperties serviceConfigProperties;
    private final ApplicationContext applicationContext;

    @Override
    public BusinessPolicy getPolicyForJourney(String journeyName) {
        JourneyConfig journeyConfig = serviceConfigProperties.getPaymentJourneys().get(journeyName);

        if (journeyConfig == null) {
            throw new IllegalArgumentException("No payment journey configured for name: " + journeyName);
        }

        List<BusinessPolicy> policies = journeyConfig.getPolicies().stream()
                .map(beanName -> applicationContext.getBean(beanName, BusinessPolicy.class))
                .collect(Collectors.toList());

        return new CompositeBusinessPolicy(policies);
    }
}
