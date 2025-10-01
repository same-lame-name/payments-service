package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
import dexter.banking.booktransfers.infrastructure.adapter.out.config.ServiceConfigProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
class BlueprintProvider {

    private final ApplicationContext applicationContext;
    private final ServiceConfigProperties serviceConfigProperties;
    private Map<String, JourneySpecification> specifications;

    BlueprintProvider(ApplicationContext applicationContext, ServiceConfigProperties serviceConfigProperties) {
        this.applicationContext = applicationContext;
        this.serviceConfigProperties = serviceConfigProperties;
    }

    @PostConstruct
    void initialize() {
        this.specifications = this.serviceConfigProperties.getJourneys().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> {
                    String journeyName = entry.getKey();
                    var properties = entry.getValue();
                    try {
                        Class<? extends JourneyBlueprint> blueprintInterface = properties.getJourneyType().getBlueprintClass();

                        // STEP 1: Eagerly construct the entire proxy graph.
                        // The constructor of the InvocationHandler now does all the recursive work.
                        JourneyBlueprint blueprintProxy = BlueprintProxyFactory.createProxy(
                                blueprintInterface,
                                properties,
                                applicationContext
                        );

                        // STEP 2: Perform a pure validation test-drive on the completed graph.
                        validateBlueprint(blueprintProxy, blueprintInterface, new HashSet<>());

                        return new JourneySpecification(journeyName, properties.getJourneyType(), blueprintProxy);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to materialize and validate blueprint for journey: '" + journeyName + "'", e);
                    }
                }
        ));
    }

    private void validateBlueprint(Object blueprintProxy, Class<? extends JourneyBlueprint> blueprintInterface, Set<Class<?>> visited) {
        if (visited.contains(blueprintInterface)) {
            return;
        }
        visited.add(blueprintInterface);

        for (Method method : blueprintInterface.getDeclaredMethods()) {
            if (method.getParameterCount() == 0) {
                try {
                    // The "test-drive" invocation.
                    Object result = method.invoke(blueprintProxy);
                    if (result instanceof JourneyBlueprint) {
                        validateBlueprint(result, (Class<? extends JourneyBlueprint>) method.getReturnType(), visited);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(String.format(
                            "Validation failed for blueprint method '%s' on interface '%s'",
                            method.getName(), blueprintInterface.getSimpleName()
                    ), e);
                }
            }
        }

        for (Class<?> superInterface : blueprintInterface.getInterfaces()) {
            if (JourneyBlueprint.class.isAssignableFrom(superInterface)) {
                validateBlueprint(blueprintProxy, (Class<? extends JourneyBlueprint>) superInterface, visited);
            }
        }
    }

    Optional<JourneySpecification> findByName(String journeyName) {
        return Optional.ofNullable(specifications.get(journeyName));
    }
}
