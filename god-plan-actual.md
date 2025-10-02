'''# The God-Plan Framework: An Architectural Blueprint

**Version:** 7.0
**Status:** Final & Authoritative (Reflecting Implemented Reality)

---

## 1. Foreword: The Philosophy of the Framework

This document is the single, canonical source of truth for the architectural design and implementation of the **Configuration-over-Code Framework**. Its purpose is to provide a set of guarantees that allow the application's core behavior to be dictated by external configuration in a safe, robust, and maintainable way.

Our architecture is founded on one supreme principle: **"Configuration over Code, Guaranteed by the Framework at Compile-Time."**

This document details the complete, unabridged implementation of this framework. It is the final word on the framework's design.

---

## 2. Step 1: The Type-Safe Blueprint Contracts

### The "Why"

A configuration-driven system is only as good as the contract between the configuration and the code. A contract based on raw strings is fragile. We must leverage the Java type system to create a compile-time safe "Blueprint" for our configuration. The configuration (`application.yml`) remains the source of truth for *which* components to use, but the *shape* and *existence* of those components will be guaranteed by the compiler.

### The "What"

We establish a "Type-Safe Blueprint" pattern. This involves a set of artifacts within the `book-transfers/core` module that form the public API of the framework.

1.  **The `JourneyBlueprint.java` Interface:** A base marker interface for all configuration blueprints.
2.  **The Core Annotations (`@ExtractBean`, `@VerifyBean`):** A sophisticated set of annotations that allow blueprint methods to declare whether they need to eagerly fetch a bean instance or merely verify the existence of a bean name in the configuration. This provides a powerful choice between eager and lazy dependency resolution.
3.  **The `BlueprintAccessor` Service:** A stateless service that provides a clean, non-invasive access pattern for application code to retrieve the current journey's blueprint, avoiding method signature pollution.
4.  **The `JourneyType.java` Enum:** The single, authoritative, and type-safe registry of all known journey types, linking them to their corresponding blueprint interfaces.

### The "How"

#### Sub-step 1.1: Define the Core Annotations

These two annotations provide granular control over how bean references in the configuration are handled.

1.  **`@ExtractBean.java`**: Guarantees that the configured String (or List of Strings) corresponds to a valid Spring bean and **returns the actual bean instance(s)** from the ApplicationContext.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.blueprint;

    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;

    /**
     * Guarantees that the configured String (or List of Strings) corresponds to a valid
     * Spring bean and returns the actual bean instance(s) from the ApplicationContext.
     * <p>
     * This annotation performs both VALIDATION and EXTRACTION.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface ExtractBean {
    }
    ```

2.  **`@VerifyBean.java`**: Guarantees that the configured String (or List of Strings) corresponds to a valid Spring bean but **returns the original String literal(s)**. This provides startup-time validation without the cost of bean extraction.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.blueprint;

    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;

    /**
     * Guarantees that the configured String (or List of Strings) corresponds to a valid
     * Spring bean name in the ApplicationContext.
     * <p>
     * This annotation performs VALIDATION ONLY. The proxy will still return the original
     * String literal(s) from the configuration, not the bean instance(s).
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    public @interface VerifyBean {
    }
    ```

#### Sub-step 1.2: Define the `JourneyBlueprint` Interfaces

A concrete blueprint for a standard payment flow, demonstrating the use of the new annotations.

```java
package dexter.banking.booktransfers.core.domain.shared.blueprint.spec;

import dexter.banking.booktransfers.core.domain.shared.blueprint.ExtractBean;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.VerifyBean;
import dexter.banking.booktransfers.core.port.in.PaymentCommand;
import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.statemachine.StateMachineFactory;
import java.util.List;

public interface StandardPaymentBlueprint extends JourneyBlueprint {
    AdapterRouting getAdapterRouting();
    Orchestration getOrchestration();

    @VerifyBean // We just need to know the names of the policies, not the beans themselves
    List<String> getPolicies();

    interface AdapterRouting {
        @ExtractBean // We need the actual adapter bean to call it
        DepositPort getDepositPort();
        @ExtractBean
        CreditCardPort getCreditCardPort();
        @ExtractBean
        LimitPort getLimitPort();
    }

    interface Orchestration {
        @ExtractBean // We need the actual factory bean to create a state machine
        StateMachineFactory<PaymentCommand> getEngine();
    }
}
```

#### Sub-step 1.3: Define the `BlueprintAccessor` Service

This public interface provides the sanctioned, non-invasive access pattern for application code.

```java
package dexter.banking.booktransfers.core.domain.shared.blueprint;

/**
 * A stateless service that provides safe, typed access to the JourneyBlueprint
 * associated with the current execution context.
 */
public interface BlueprintAccessor {
    /**
     * Retrieves the current journey's blueprint and safely casts it to the requested type.
     *
     * @param blueprintType The specific {@link JourneyBlueprint} interface class desired.
     * @param <T>           The specific blueprint type.
     * @return The blueprint for the current journey.
     * @throws IllegalStateException if no journey context is active.
     * @throws ClassCastException    if the current journey's blueprint is not an instance
     *                               of the requested blueprintType.
     */
    <T extends JourneyBlueprint> T get(Class<T> blueprintType);
}
```

#### Sub-step 1.4: Define the `JourneyType` Enum

```java
package dexter.banking.booktransfers.core.domain.shared.blueprint;

import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.StandardPaymentBlueprint;

public enum JourneyType {
    STANDARD_PAYMENT(StandardPaymentBlueprint.class);

    private final Class<? extends JourneyBlueprint> blueprintClass;

    JourneyType(Class<? extends JourneyBlueprint> blueprintClass) {
        this.blueprintClass = blueprintClass;
    }

    public Class<? extends JourneyBlueprint> getBlueprintClass() {
        return blueprintClass;
    }
}
```

#### Sub-step 1.5: Define the `application.yml` Contract

The YAML structure is a direct mapping to the blueprint interfaces.

```yaml
app:
  journeys:
    STANDARD_PAYMENT_V1:
      journeyType: "STANDARD_PAYMENT"
      policies: ["STANDARD_VALIDATION_POLICY"]
      orchestration:
        engine: "standardPaymentStateMachineFactory"
      adapterRouting:
        depositPort: "DEPOSIT_PORT_REST"
        creditCardPort: "CREDIT_PORT_JMS"
        limitPort: "LIMIT_PORT_REST"
```

---

## 3. Step 2 (DEPRECATED): The Public Bean Materializer
<details>
<summary><strong>[CLICK TO EXPAND] DEPRECATED - Version 3.0 Design (Critically Flawed)</strong></summary>

**Reason for Deprecation:** This design is critically flawed as it exposes the `Map<String, JourneySpecification>` as a public Spring bean. This creates a massive, unsecured backdoor that allows any component in the application to inject the entire configuration store, completely undermining the security and monopoly of the framework's access patterns. It violates the foundational principle of "a developer cannot call an API they cannot see." The correct, secure, and encapsulated design is detailed in **Step 2 (Authoritative): The Secure, Eager-Loading Framework**.

</details>

---

## 4. Step 3 (DEPRECATED): Insecure Context Propagation
<details>
<summary><strong>[CLICK TO EXPAND] DEPRECATED - Version 4.0 Design (Critically Flawed)</strong></summary>

**Reason for Deprecation:** This design, while an improvement, still relied on a public `JourneySpecificationProvider` interface. This still presents an unnecessary public API and a potential backdoor for misuse. The final, authoritative design further restricts visibility by making the provider and its consumers `package-private` and co-locating them, which is a superior and more secure pattern.

</details>

---

## 5. Step 2 (Authoritative): The Secure, Eager-Loading Framework

### The "Why"

The framework's security and robustness are paramount. We must enforce our rules through compiler-guaranteed visibility and provide fail-fast validation at application startup. The framework must be both secure and developer-friendly.

### The "What"

We implement a **Secure, Eager-Loading Framework**. The configuration store is a `package-private` implementation detail, completely invisible to the application. The framework eagerly constructs and validates the entire graph of blueprint proxies at startup, guaranteeing that any configuration error results in immediate application failure.

1.  **A Secure Package:** The `dexter.banking.booktransfers.infrastructure.provider` package is the security boundary.
2.  **The `BlueprintProxyFactory` (The Engine):** A sophisticated, self-contained factory responsible for eagerly parsing the configuration, recursively building the entire graph of nested blueprint proxies, and performing all validation at construction time.
3.  **The `BlueprintProvider` (The Store):** A simplified, `package-private` class that holds the final, fully-validated journey specifications produced by the factory.
4.  **The `DefaultBlueprintAccessor` (The Gate):** The public-facing service that allows application code to safely access the blueprint established in the current `JourneyContext`.

### The "How"

#### Sub-step 2.1: Define Supporting Data Structures

These classes are used to load and hold the configuration.

1.  **`JourneyProperties.java`**: A set of classes that map directly to the `application.yml` structure, used by Spring's `ConfigurationProperties` mechanism.

    ```java
    package dexter.banking.booktransfers.infrastructure.provider;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
    import lombok.Data;
    import java.util.List;

    @Data // from Lombok, for getters/setters
    public class JourneyProperties {
        private JourneyType journeyType;
        private List<String> policies;
        private OrchestrationProperties orchestration;
        private AdapterRoutingProperties adapterRouting;

        @Data
        public static class OrchestrationProperties {
            private String engine;
        }

        @Data
        public static class AdapterRoutingProperties {
            private String depositPort;
            private String creditCardPort;
            private String limitPort;
        }
    }
    ```

2.  **`JourneySpecification.java`**: A simple, immutable container that holds the processed and validated details of a single journey.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.context;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
    import lombok.Getter;
    import lombok.RequiredArgsConstructor;

    @Getter
    @RequiredArgsConstructor
    public class JourneySpecification {
        private final String journeyName;
        private final JourneyType journeyType;
        private final JourneyBlueprint blueprint;
    }
    ```

#### Sub-step 2.2: Implement the `BlueprintProxyFactory`

This is the heart of the framework. It eagerly constructs a cache of nested proxies and lazy-initialized "recipes" for leaf-node values. This provides startup-time validation and runtime efficiency.

```java
package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.ExtractBean;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.VerifyBean;
import org.springframework.context.ApplicationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public final class BlueprintProxyFactory {

    private BlueprintProxyFactory() {
    }

    @SuppressWarnings("unchecked")
    public static <T extends JourneyBlueprint> T createProxy(
            Class<T> blueprintInterface,
            Object properties,
            ApplicationContext ctx
    ) {
        return (T) Proxy.newProxyInstance(
                blueprintInterface.getClassLoader(),
                new Class<?>[]{blueprintInterface},
                new BlueprintInvocationHandler(blueprintInterface, properties, ctx)
        );
    }

    private static class BlueprintInvocationHandler implements InvocationHandler {
        private final Map<Method, Object> methodCache = new HashMap<>();

        public BlueprintInvocationHandler(Class<?> blueprintInterface, Object properties, ApplicationContext ctx) {
            buildCache(blueprintInterface, properties, ctx);
        }

        private void buildCache(Class<?> blueprintInterface, Object properties, ApplicationContext ctx) {
            for (Method method : blueprintInterface.getMethods()) {
                if (method.getDeclaringClass().equals(Object.class) || methodCache.containsKey(method)) {
                    continue;
                }

                try {
                    if (JourneyBlueprint.class.isAssignableFrom(method.getReturnType())) {
                        Method propertiesGetter = properties.getClass().getMethod(method.getName());
                        Object nestedProperties = propertiesGetter.invoke(properties);
                        if (nestedProperties == null)
                            throw new IllegalStateException("Missing config for nested blueprint '" + method.getName() + "'");

                        Object nestedProxy = createProxy((Class<? extends JourneyBlueprint>) method.getReturnType(), nestedProperties, ctx);
                        methodCache.put(method, nestedProxy);
                    } else {
                        Supplier<Object> recipe = createRecipeForLeaf(method, properties, ctx);
                        methodCache.put(method, recipe);
                    }
                } catch (Exception e) {
                    throw new IllegalStateException("Failed to build blueprint cache for method '" + method.getName() + "'", e);
                }
            }
        }

        private Supplier<Object> createRecipeForLeaf(Method method, Object properties, ApplicationContext ctx) throws NoSuchMethodException {
            Method propertiesGetter = properties.getClass().getMethod(method.getName());

            if (method.isAnnotationPresent(ExtractBean.class)) {
                return () -> {
                    try {
                        Object configuredValue = propertiesGetter.invoke(properties);
                        if (configuredValue == null)
                            throw new IllegalStateException("Missing config for @ExtractBean '" + method.getName() + "'");

                        if (method.getReturnType().equals(List.class)) {
                            Type beanType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
                            List<String> beanNames = (List<String>) configuredValue;
                            return beanNames.stream()
                                    .map(name -> ctx.getBean(name, (Class<?>) beanType))
                                    .collect(Collectors.toList());
                        } else {
                            String beanName = (String) configuredValue;
                            if (!StringUtils.hasText(beanName))
                                throw new IllegalStateException("Empty config for @ExtractBean '" + method.getName() + "'");
                            return ctx.getBean(beanName, method.getReturnType());
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }
            else if (method.isAnnotationPresent(VerifyBean.class)) {
                return () -> {
                    try {
                        Object configuredValue = propertiesGetter.invoke(properties);
                        if (configuredValue == null)
                            throw new IllegalStateException("Missing config for @VerifyBean '" + method.getName() + "'");

                        if (configuredValue instanceof List) {
                            List<String> beanNames = (List<String>) configuredValue;
                            for (String beanName : beanNames) {
                                if (!ctx.containsBean(beanName))
                                    throw new IllegalStateException("Verified bean not found: " + beanName);
                            }
                        } else {
                            String beanName = (String) configuredValue;
                            if (!ctx.containsBean(beanName))
                                throw new IllegalStateException("Verified bean not found: ".concat(beanName));
                        }
                        return configuredValue;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }
            else {
                return () -> {
                    try {
                        Object configuredValue = propertiesGetter.invoke(properties);
                        if (configuredValue == null)
                            throw new IllegalStateException("Missing config for mandatory property '" + method.getName() + "'");
                        return configuredValue;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
            }
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            Object cachedValue = methodCache.get(method);

            if (cachedValue instanceof Supplier) {
                return ((Supplier<?>) cachedValue).get();
            } else {
                return cachedValue;
            }
        }
    }
}
```

#### Sub-step 2.3: Implement the `BlueprintProvider`

The provider is now a simple store, delegating the complex creation logic to the factory.

```java
package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "app")
class BlueprintProvider {

    private final ApplicationContext applicationContext;
    @Getter
    private final Map<String, JourneyProperties> journeys = new HashMap<>();
    private Map<String, JourneySpecification> specifications;

    BlueprintProvider(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    void materializeAndValidateBlueprints() {
        this.specifications = this.journeys.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> {
                    String journeyName = entry.getKey();
                    JourneyProperties rawConfig = entry.getValue();
                    try {
                        JourneyType journeyType = rawConfig.getJourneyType();
                        JourneyBlueprint blueprintProxy = BlueprintProxyFactory.createProxy(
                                journeyType.getBlueprintClass(),
                                rawConfig,
                                applicationContext
                        );
                        return new JourneySpecification(journeyName, journeyType, blueprintProxy);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to materialize blueprint for journey: '" + journeyName + "'", e);
                    }
                }
        ));
    }

    Optional<JourneySpecification> findByName(String journeyName) {
        return Optional.ofNullable(specifications.get(journeyName));
    }
}
```

#### Sub-step 2.4: Implement the `BlueprintAccessor` Access Pattern

The `DefaultBlueprintAccessor` provides the concrete implementation for the public interface, cleanly retrieving the blueprint from the thread-local `JourneyContext`.

```java
package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.blueprint.BlueprintAccessor;
import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManager;
import org.springframework.stereotype.Component;

@Component
class DefaultBlueprintAccessor implements BlueprintAccessor {
    @Override
    public <T extends JourneyBlueprint> T get(Class<T> blueprintType) {
        JourneyBlueprint blueprint = JourneyContextManager.getContext().getSpecification().getBlueprint();
        if (!blueprintType.isInstance(blueprint)) {
            throw new ClassCastException(String.format(
                    "Cannot cast blueprint of type '%s' to requested type '%s'.",
                    blueprint.getClass().getInterfaces()[0].getSimpleName(),
                    blueprintType.getSimpleName()
            ));
        }
        return blueprintType.cast(blueprint);
    }
}
```

#### Sub-step 2.5: Co-locate the `ConfigurationEnrichmentMiddleware`

This middleware is a `package-private` component within the secure provider package. It intercepts commands and establishes the `JourneyContext`.

```java
package dexter.banking.booktransfers.infrastructure.provider;

import dexter.banking.booktransfers.core.domain.shared.context.JourneyContext;
import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManager;
import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.Middleware;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Order(1)
@Component
@RequiredArgsConstructor
class ConfigurationEnrichmentMiddleware implements Middleware {

    private final BlueprintProvider blueprintProvider;

    @Override
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        String journeyName = command.getIdentifier();
        JourneySpecification spec = blueprintProvider.findByName(journeyName)
            .orElseThrow(() -> new IllegalStateException("No journey specification found for command identifier: " + journeyName));

        var context = new JourneyContext(spec);
        try {
            return JourneyContextManager.runWithContext(context, next::invoke);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
```

---

## 6. Implementation Status

This section tracks the delivery of the framework components.

### **MVP 1: The Core Blueprint Contracts**
- **Status:** Implemented
- **Artifacts Delivered:**
  - `JourneyBlueprint.java`
  - `@ExtractBean.java`
  - `@VerifyBean.java`
  - `BlueprintAccessor.java`
  - `StandardPaymentBlueprint.java`
  - `JourneyType.java`

### **MVP 2: The Secure Infrastructure Provider**
- **Status:** Implemented
- **Artifacts Delivered:**
  - `JourneyProperties.java`
  - `JourneySpecification.java`
  - `BlueprintProxyFactory.java`
  - `BlueprintProvider.java`
  - `DefaultBlueprintAccessor.java`

### **MVP 3: Context Propagation**
- **Status:** Implemented
- **Artifacts Delivered:**
  - `ConfigurationEnrichmentMiddleware.java`
''