# The God-Plan Framework (Simpler): An Architectural Blueprint

**Version:** 1.4
**Status:** Proposed

---

## 1. The "Why": The Case for a Dependency-Free, Spring-Native Framework

Previous proposals, while iterative improvements, still contained unacceptable architectural flaws:

1.  **Unnecessary Dependencies:** The use of a third-party library (`io.github.classgraph`) for classpath scanning introduced an external dependency for a task that should be handled by the core Spring Framework.
2.  **Brittle Configuration:** Hard-coding package names (`dexter.banking.booktransfers.infrastructure.provider`) into the framework is a critical anti-pattern. It makes the framework fragile and difficult to refactor.
3.  **Stateful Singleton Conflict:** The designs did not cleanly resolve the conflict between Spring's default singleton bean scope and the framework's need for unique, stateful blueprint instances for each configured journey.

This proposal corrects these fundamental flaws by adopting a robust, dependency-free architecture that uses standard Spring patterns.

---

## 2. The "What": The New Architectural Vision

The core philosophy remains unchanged: **"Configuration over Code, Guaranteed by the Framework at Compile-Time."**

This is achieved through a refined set of pillars:

1.  **Type-Safe Properties:** Using Spring Boot's `@ConfigurationProperties` to bind the `application.yml` to a hierarchy of strongly-typed POJOs. (Unchanged)
2.  **The Blueprint Factory Pattern:** We will introduce a `BlueprintFactory` interface. The concrete implementations of our blueprints will now be **singleton factories** managed by Spring. Their sole purpose is to create new, stateful, non-bean instances of a blueprint for each journey.
3.  **Spring-Native Discovery:** The `BlueprintProvider` will use the `ApplicationContext` directly to discover all `BlueprintFactory` beans. This is a clean, dependency-free approach that leverages the core capabilities of the Spring container.
4.  **Deep, Scoped Validation:** A precise, top-down recursive validation traverses the blueprint interface graph, ensuring correctness and completeness at startup. (Unchanged)

---

## 3. The "How": The Detailed Implementation Plan

This section provides the detailed, file-by-file implementation plan. It shows the **final state** of all new and modified files.

### **Phase 1: Establish Type-Safe Configuration Properties**

(This phase is unchanged and provides the foundation for the framework.)

*   **`JourneysProperties.java`**: The root `@ConfigurationProperties` class for `app.journeys`.
*   **`JourneyProperties.java`**: A POJO representing a single journey's configuration.
*   **`AdapterRoutingProperties.java` & `OrchestrationProperties.java`**: Nested POJOs for structured configuration.
*   **`PropertiesConfigurationAdapter.java`**: The `@Configuration` class that enables `JourneysProperties`.

---
### **Phase 2: The Blueprint Factory Pattern**

**2.1. New File: `BlueprintFactory.java`**
*   **Path:** `.../infrastructure/provider/BlueprintFactory.java`
*   **Final Content:**
    ```java
    package dexter.banking.booktransfers.infrastructure.provider;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import dexter.banking.booktransfers.infrastructure.adapter.out.config.JourneyProperties;
    import org.springframework.context.ApplicationContext;

    /**
     * Defines the contract for a factory that creates stateful JourneyBlueprint instances.
     * The factory itself is a singleton Spring bean, but the instances it creates are not.
     */
    public interface BlueprintFactory {
        JourneyBlueprint createInstance(String journeyName, JourneyProperties properties, ApplicationContext context);
    }
    ```

**2.2. Modified File: `StandardPaymentBlueprintImpl.java`**
*   **Path:** `.../infrastructure/provider/StandardPaymentBlueprintImpl.java`
*   **Final Content:**
    ```java
    package dexter.banking.booktransfers.infrastructure.provider;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.BeanReference;
    import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.StandardPaymentBlueprint;
    import dexter.banking.booktransfers.core.domain.shared.validation.ValidationGroup;
    import dexter.banking.booktransfers.core.port.out.CreditPort;
    import dexter.banking.booktransfers.core.port.out.DepositPort;
    import dexter.banking.booktransfers.core.port.out.LimitPort;
    import dexter.banking.booktransfers.infrastructure.adapter.out.config.AdapterRoutingProperties;
    import dexter.banking.booktransfers.infrastructure.adapter.out.config.JourneyProperties;
    import dexter.banking.booktransfers.infrastructure.adapter.out.config.OrchestrationProperties;
    import dexter.banking.statemachine.StateMachineFactory;
    import lombok.RequiredArgsConstructor;
    import org.springframework.context.ApplicationContext;
    import org.springframework.stereotype.Component;

    import java.util.List;

    @Component // This is now a discoverable singleton bean that acts as a factory.
    public class StandardPaymentBlueprintImpl implements StandardPaymentBlueprint, BlueprintFactory {

        @Override
        public JourneyBlueprint createInstance(String journeyName, JourneyProperties properties, ApplicationContext context) {
            return new StatefulStandardPaymentBlueprint(journeyName, properties, context);
        }

        // --- Methods from JourneyBlueprint ---
        // These methods throw exceptions because they should not be called on the singleton factory itself.
        @Override public String journeyName() { throw new UnsupportedOperationException("Cannot call on factory"); }
        @Override public List<ValidationGroup> validationGroups() { throw new UnsupportedOperationException("Cannot call on factory"); }
        @Override public List<String> dataCollectors() { throw new UnsupportedOperationException("Cannot call on factory"); }
        @Override public List<String> businessRules() { throw new UnsupportedOperationException("Cannot call on factory"); }
        @Override public AdapterRoutingBlueprint adapterRouting() { throw new UnsupportedOperationException("Cannot call on factory"); }
        @Override public OrchestrationBlueprint orchestration() { throw new UnsupportedOperationException("Cannot call on factory"); }

        /**
         * This private, non-bean class holds the actual state and logic for a specific journey instance.
         */
        @RequiredArgsConstructor
        private static class StatefulStandardPaymentBlueprint implements StandardPaymentBlueprint {
            private final String journeyName;
            private final JourneyProperties properties;
            private final ApplicationContext applicationContext;

            @Override
            public String journeyName() {
                return this.journeyName;
            }

            @Override
            public List<ValidationGroup> validationGroups() {
                return properties.getValidationGroups();
            }

            @Override
            public List<String> dataCollectors() {
                return properties.getDataCollectors();
            }

            @Override
            public List<String> businessRules() {
                return properties.getBusinessRules();
            }

            @Override
            public AdapterRoutingBlueprint adapterRouting() {
                return new AdapterRoutingBlueprintImpl(properties.getAdapterRouting(), applicationContext);
            }

            @Override
            public OrchestrationBlueprint orchestration() {
                return new OrchestrationBlueprintImpl(properties.getOrchestration(), applicationContext);
            }

            @RequiredArgsConstructor
            private static class AdapterRoutingBlueprintImpl implements AdapterRoutingBlueprint {
                private final AdapterRoutingProperties properties;
                private final ApplicationContext applicationContext;

                @Override @BeanReference
                public DepositPort depositPort() {
                    return applicationContext.getBean(properties.getDepositPort(), DepositPort.class);
                }

                @Override @BeanReference
                public CreditPort creditPort() {
                    return applicationContext.getBean(properties.getCreditPort(), CreditPort.class);
                }

                @Override @BeanReference
                public LimitPort limitPort() {
                    return applicationContext.getBean(properties.getLimitPort(), LimitPort.class);
                }
            }

            @RequiredArgsConstructor
            private static class OrchestrationBlueprintImpl implements OrchestrationBlueprint {
                private final OrchestrationProperties properties;
                private final ApplicationContext applicationContext;

                @Override @BeanReference
                public StateMachineFactory engine() {
                    return applicationContext.getBean(properties.getEngine(), StateMachineFactory.class);
                }
            }
        }
    }
    ```

---
### **Phase 3: Refactor the Framework Core**

**3.1. Modified File: `BlueprintProvider.java`**
*   **Path:** `.../infrastructure/provider/BlueprintProvider.java`
*   **Final Content:**
    ```java
    package dexter.banking.booktransfers.infrastructure.provider;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.BeanReference;
    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import dexter.banking.booktransfers.infrastructure.adapter.out.config.JourneysProperties;
    import jakarta.annotation.PostConstruct;
    import org.springframework.context.ApplicationContext;
    import org.springframework.stereotype.Component;

    import java.lang.reflect.Method;
    import java.util.Arrays;
    import java.util.List;
    import java.util.Map;
    import java.util.Optional;
    import java.util.stream.Collectors;

    @Component
    class BlueprintProvider {

        private final ApplicationContext applicationContext;
        private final JourneysProperties journeysProperties;
        private Map<String, JourneySpecificationDeprecated> specifications;
        private Map<Class<? extends JourneyBlueprint>, BlueprintFactory> factoryRegistry;

        BlueprintProvider(ApplicationContext applicationContext, JourneysProperties journeysProperties) {
            this.applicationContext = applicationContext;
            this.journeysProperties = journeysProperties;
        }

        @PostConstruct
        void initialize() {
            buildFactoryRegistry();
            materializeAndValidateBlueprints();
        }

        private void buildFactoryRegistry() {
            // Discover all beans that implement the root JourneyBlueprint interface.
            Map<String, JourneyBlueprint> foundBeans = applicationContext.getBeansOfType(JourneyBlueprint.class);

            this.factoryRegistry = Arrays.stream(JourneyType.values())
                    .collect(Collectors.toMap(
                            JourneyType::getBlueprintClass,
                            journeyType -> {
                                Class<? extends JourneyBlueprint> blueprintInterface = journeyType.getBlueprintClass();
                                // For each enum, find the single bean that implements its specific interface.
                                List<JourneyBlueprint> implementations = foundBeans.values().stream()
                                        .filter(bean -> blueprintInterface.isAssignableFrom(bean.getClass()))
                                        .toList();

                                if (implementations.isEmpty()) {
                                    throw new IllegalStateException("No @Component bean found that implements: " + blueprintInterface.getSimpleName());
                                }
                                if (implementations.size() > 1) {
                                    throw new IllegalStateException("Multiple @Component beans found that implement: " + blueprintInterface.getSimpleName());
                                }
                                JourneyBlueprint factory = implementations.get(0);
                                if (!(factory instanceof BlueprintFactory)) {
                                    throw new IllegalStateException("Blueprint implementation " + factory.getClass().getSimpleName() + " must also implement BlueprintFactory");
                                }
                                return (BlueprintFactory) factory;
                            }
                    ));
        }

        private void materializeAndValidateBlueprints() {
            this.specifications = this.journeysProperties.getJourneys().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> {
                    String journeyName = entry.getKey();
                    var properties = entry.getValue();
                    try {
                        Class<? extends JourneyBlueprint> blueprintInterface = properties.getJourneyType().getBlueprintClass();
                        BlueprintFactory factory = factoryRegistry.get(blueprintInterface);
                        if (factory == null) {
                            throw new IllegalStateException("No implementation factory found for interface: " + blueprintInterface.getSimpleName());
                        }

                        // Use the factory to create a new, stateful instance for this specific journey.
                        JourneyBlueprint blueprint = factory.createInstance(journeyName, properties, applicationContext);

                        validateBlueprint(blueprint, blueprintInterface);

                        return new JourneySpecificationDeprecated(journeyName, properties.getJourneyType(), blueprint);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to materialize and validate blueprint for journey: '" + journeyName + "'", e);
                    }
                }
            ));
        }

        private void validateBlueprint(Object blueprintInstance, Class<? extends JourneyBlueprint> blueprintInterface) {
            for (Method method : blueprintInterface.getDeclaredMethods()) {
                if (method.getParameterCount() == 0) {
                    try {
                        Object result = method.invoke(blueprintInstance);

                        if (method.isAnnotationPresent(BeanReference.class) && result == null) {
                            throw new IllegalStateException("Bean reference method returned null, but a bean instance was expected.");
                        }

                        if (JourneyBlueprint.class.isAssignableFrom(method.getReturnType()) && result != null) {
                            validateBlueprint(result, (Class<? extends JourneyBlueprint>) method.getReturnType());
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
                    validateBlueprint(blueprintInstance, (Class<? extends JourneyBlueprint>) superInterface);
                }
            }
        }

        Optional<JourneySpecificationDeprecated> findByName(String journeyName) {
            return Optional.ofNullable(specifications.get(journeyName));
        }
    }
    ```

---

## 4. Justification: Honoring the Original God-Plan

This design is the most robust and architecturally sound version proposed.

*   **Spring-Native:** The framework is now free of third-party dependencies and hard-coded values, relying solely on the `ApplicationContext` for discovery. This is clean, maintainable, and idiomatic for a Spring environment.
*   **Correctly Scoped State:** The Factory Pattern correctly resolves the conflict between singleton beans and stateful instances. The framework is now both discoverable and capable of handling multiple journey configurations correctly.
*   **Enhanced Safety & Extensibility:** By using the `JourneyType` enum as the authoritative registry and discovering its corresponding factory bean, we create a powerful and safe extension mechanism. Adding a new journey type is a matter of:
    1.  Defining the new blueprint interface.
    2.  Adding a new `JourneyType` enum constant.
    3.  Creating a single `@Component` class that implements both the new interface and `BlueprintFactory`.
*   **Maintainability & Debuggability:** The framework remains transparent and debuggable. Developers can place breakpoints inside the factory or the stateful instance to understand behavior.

This revised plan delivers on the original architectural goals with a pragmatic, robust, and truly extensible implementation that adheres to established design patterns.
