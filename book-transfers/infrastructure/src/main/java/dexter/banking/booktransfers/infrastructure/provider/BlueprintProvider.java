package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.BeanReference;
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

                    // Create the top-level proxy, now backed by the type-safe properties object.
                    JourneyBlueprint blueprintProxy = BlueprintProxyFactory.createProxy(
                        blueprintInterface,
                        properties,
                        applicationContext
                    );

                    // Perform deep, scoped validation on the proxy itself.
                    validateBlueprint(blueprintProxy, blueprintInterface, new HashSet<>());

                    return new JourneySpecification(journeyName, properties.getJourneyType(), blueprintProxy);
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to materialize and validate blueprint for journey: '" + journeyName + "'", e);
                }
            }
        ));
    }

    private void validateBlueprint(
        Object blueprintProxy,
        Class<? extends JourneyBlueprint> blueprintInterface,
        Set<Class<?>> visited
    ) {
        // 1. Prevent infinite loops by tracking visited interfaces.
        if (visited.contains(blueprintInterface)) {
            return;
        }
        visited.add(blueprintInterface);

        // 2. Validate all methods declared on the CURRENT interface level.
        for (Method method : blueprintInterface.getDeclaredMethods()) {
            if (method.getParameterCount() == 0) {
                try {
                    // Invoking the method on the proxy triggers the InvocationHandler.
                    Object result = method.invoke(blueprintProxy);

                    // 3. THE KEY CHANGE: Recurse DOWN into the nested blueprint.
                    if (JourneyBlueprint.class.isAssignableFrom(method.getReturnType()) && result != null) {
                        validateBlueprint(result, (Class<? extends JourneyBlueprint>) method.getReturnType(), visited);
                    }
                    // This also implicitly validates @BeanReference methods, as the proxy's handler will throw
                    // an exception if getBean fails. We add a null check for completeness.
                    else if (method.isAnnotationPresent(BeanReference.class) && result == null) {
                        throw new IllegalStateException("Bean reference method returned null.");
                    }
                } catch (Exception e) {
                    throw new IllegalStateException(String.format(
                        "Validation failed for blueprint method '%s' on interface '%s'",
                        method.getName(), blueprintInterface.getSimpleName()
                    ), e);
                }
            }
        }

        // 4. After validating the current level, recurse UP to parent interfaces.
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
