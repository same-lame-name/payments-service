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

                    // STEP 1: Create the root proxy.
                    JourneyBlueprint blueprintProxy = BlueprintProxyFactory.createProxy(
                        blueprintInterface,
                        properties,
                        applicationContext
                    );

                    // STEP 2: Eagerly construct the entire nested proxy graph.
                    constructProxyGraph(blueprintProxy, blueprintInterface, new HashSet<>());

                    // STEP 3: Perform a full validation test-drive on the completed graph.
                    validateBlueprint(blueprintProxy, blueprintInterface, new HashSet<>());

                    return new JourneySpecification(journeyName, properties.getJourneyType(), blueprintProxy);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to materialize and validate blueprint for journey: '" + journeyName + "'", e);
                }
            }
        ));
    }

    /**
     * Traverses the blueprint graph with the SOLE purpose of triggering the
     * just-in-time creation of all nested proxy objects.
     */
    private void constructProxyGraph(Object blueprintProxy, Class<? extends JourneyBlueprint> blueprintInterface, Set<Class<?>> visited) {
        if (visited.contains(blueprintInterface)) {
            return;
        }
        visited.add(blueprintInterface);

        // Traverse down the composition graph.
        for (Method method : blueprintInterface.getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && JourneyBlueprint.class.isAssignableFrom(method.getReturnType())) {
                try {
                    Object nestedProxy = method.invoke(blueprintProxy); // This triggers the InvocationHandler to create the nested proxy.
                    if (nestedProxy != null) {
                        constructProxyGraph(nestedProxy, (Class<? extends JourneyBlueprint>) method.getReturnType(), visited);
                    }
                } catch (Exception e) {
                    // This wrap is critical. An error during construction is a construction error.
                    throw new IllegalStateException("Failed to construct nested proxy for method '" + method.getName() + "'", e);
                }
            }
        }

        // Traverse up the inheritance graph.
        for (Class<?> superInterface : blueprintInterface.getInterfaces()) {
            if (JourneyBlueprint.class.isAssignableFrom(superInterface)) {
                constructProxyGraph(blueprintProxy, (Class<? extends JourneyBlueprint>) superInterface, visited);
            }
        }
    }

    /**
     * Traverses the fully constructed blueprint graph to validate every method.
     * This method can now be modified independently to skip certain validations.
     */
    private void validateBlueprint(Object blueprintProxy, Class<? extends JourneyBlueprint> blueprintInterface, Set<Class<?>> visited) {
        if (visited.contains(blueprintInterface)) {
            return;
        }
        visited.add(blueprintInterface);

        // Validate all methods on the current interface.
        for (Method method : blueprintInterface.getDeclaredMethods()) {
            if (method.getParameterCount() == 0) {
                try {
                    // The "test-drive" invocation.
                    Object result = method.invoke(blueprintProxy);

                    // If the method returns a nested blueprint, recurse down to validate it too.
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

        // Recurse up the inheritance graph.
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
