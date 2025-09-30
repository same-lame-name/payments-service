package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "app")
class BlueprintProvider {

    private final ApplicationContext applicationContext;
    private final Map<String, Map<String, Object>> journeys = new java.util.HashMap<>();
    private Map<String, JourneySpecification> specifications;

    BlueprintProvider(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Map<String, Map<String, Object>> getJourneys() {
        return journeys;
    }

    @PostConstruct
    void materializeAndValidateBlueprints() {
        this.specifications = this.journeys.entrySet().stream().collect(Collectors.toUnmodifiableMap(
            Map.Entry::getKey,
            entry -> {
                String journeyName = entry.getKey();
                Map<String, Object> rawConfig = entry.getValue();
                try {
                    JourneyType journeyType = JourneyType.valueOf((String) rawConfig.get("journeyType"));
                    Class<? extends JourneyBlueprint> blueprintClass = journeyType.getBlueprintClass();
                    JourneyBlueprint blueprintProxy = BlueprintProxyFactory.createProxy(blueprintClass, rawConfig, applicationContext);

                    // EAGER VALIDATION STEP
                    validateBlueprintProxy(blueprintProxy);

                    return new JourneySpecification(journeyName, journeyType, rawConfig, blueprintProxy);
                } catch (Exception e) {
                    // This catch block now correctly fails application startup.
                    throw new IllegalStateException("Failed to materialize and validate blueprint for journey: '" + journeyName + "'", e);
                }
            }
        ));
    }

    /**
     * Recursively traverses a blueprint proxy, invoking every method to ensure
     * the underlying configuration is valid and complete at startup.
     */
    private void validateBlueprintProxy(JourneyBlueprint proxy) {
        List<Method> methods = Arrays.asList(proxy.getClass().getInterfaces()[0].getDeclaredMethods());

        for (Method method : methods) {
            // We only care about methods with no arguments, which define the properties.
            if (method.getParameterCount() == 0) {
                try {
                    Object result = method.invoke(proxy);
                    // If the result is a nested blueprint, recurse.
                    if (result instanceof JourneyBlueprint nestedBlueprint) {
                        validateBlueprintProxy(nestedBlueprint);
                    }
                } catch (Exception e) {
                    // Wrap exception to provide a clear path to the configuration error.
                    throw new IllegalStateException(String.format(
                        "Validation failed for blueprint method '%s' on interface '%s'",
                        method.getName(), method.getDeclaringClass().getSimpleName()
                    ), e);
                }
            }
        }
    }

    Optional<JourneySpecification> findByName(String journeyName) {
        return Optional.ofNullable(specifications.get(journeyName));
    }
}
