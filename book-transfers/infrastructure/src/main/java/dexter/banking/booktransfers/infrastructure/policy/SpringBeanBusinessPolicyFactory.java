package dexter.banking.booktransfers.infrastructure.policy;

import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.port.out.BusinessPolicyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * The concrete Driven Adapter for the BusinessPolicyFactory.
 * This adapter uses the Spring ApplicationContext to look up named policy beans,
 * centralizing the logic for policy composition.
 */
@Component
@RequiredArgsConstructor
class SpringBeanBusinessPolicyFactory implements BusinessPolicyFactory {

    private final ApplicationContext applicationContext;

    @Override
    public BusinessPolicy create(JourneySpecification spec) {

        List<BusinessPolicy> policies = Optional.of(spec)
                .map(JourneySpecification::policies)
                .stream()
                .flatMap(List::stream)
                .map(beanName -> applicationContext.getBean(beanName, BusinessPolicy.class))
                .toList();

        return new CompositeBusinessPolicy(policies);
    }
}
