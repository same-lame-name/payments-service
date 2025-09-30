# The God-Plan Framework: An Architectural Blueprint

**Version:** 3.0
**Status:** Final & Authoritative

---

## 1. Foreword: The Philosophy of the Framework

This document is the single, canonical source of truth for the architectural design and implementation of the **Configuration-over-Code Framework**. Its purpose is to provide a set of guarantees that allow the application's core behavior to be dictated by external configuration in a safe, robust, and maintainable way.

Our architecture is founded on one supreme principle: **"Configuration over Code, Guaranteed by the Framework at Compile-Time."**

A simple typo in a configuration file must not lead to a production `NullPointerException`. Therefore, this framework provides an ironclad, multi-layered, and non-bypassable guarantee that this configuration is always valid, complete, and correctly applied. This guarantee is enforced by the Java compiler wherever possible, moving failure detection from runtime to compile-time.

This document details the complete, unabridged implementation of this framework. It is the final word on the framework's design.

---

## 2. Step 1: The Type-Safe Blueprint Contracts

### The "Why"

A configuration-driven system is only as good as the contract between the configuration and the code. A contract based on raw strings is fragile. We must leverage the Java type system to create a compile-time safe "Blueprint" for our configuration. The configuration (`application.yml`) remains the source of truth for *which* components to use, but the *shape* and *existence* of those components will be guaranteed by the compiler.

### The "What"

We will establish a "Type-Safe Blueprint" pattern. This involves a set of artifacts within the `book-transfers/core` module that form the public API of the framework.

1.  **The `JourneyType.java` Enum:** The single, authoritative, and type-safe registry of all known journey types.
2.  **The `JourneyBlueprint.java` Interfaces:** A hierarchy of pure Java interfaces that declaratively define the *shape* of configuration required for a journey.
3.  **The Core Annotations:** A set of annotations (`@InJourney`, `@WithJourneyContext`) that form the developer's interface for interacting with the framework.

### The "How"

#### Sub-step 1.1: Define the `JourneyType` Enum

This enum is the linchpin of the type-safe system, providing a single, discoverable, and compile-time safe registry of all possible journeys. It eliminates fragile, fully-qualified class names from the configuration.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/blueprint/JourneyType.java`
2.  **Visibility:** `public`
3.  **Action:** Create the enum. Each constant represents a journey and holds a reference to its corresponding blueprint interface `Class`.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.blueprint;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.StandardPaymentBlueprint;
    import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.WalletJourneyBlueprint;

    public enum JourneyType {
        STANDARD_PAYMENT(StandardPaymentBlueprint.class),
        WALLET_JOURNEY(WalletJourneyBlueprint.class);

        private final Class<? extends JourneyBlueprint> blueprintClass;

        JourneyType(Class<? extends JourneyBlueprint> blueprintClass) {
            this.blueprintClass = blueprintClass;
        }

        public Class<? extends JourneyBlueprint> getBlueprintClass() {
            return blueprintClass;
        }
    }
    ```

#### Sub-step 1.2: Define the `JourneyBlueprint` Interfaces

These interfaces are the core of our compile-time contract system.

1.  **File Location (Base Interface):** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/blueprint/JourneyBlueprint.java`
2.  **Visibility:** `public`
3.  **Action:** Create the base marker interface.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.blueprint;

    import dexter.banking.booktransfers.core.domain.shared.validation.ValidationGroup;
    import java.util.List;

    public interface JourneyBlueprint {
        String journeyName();
        List<ValidationGroup> validationGroups();
        List<String> dataCollectors();
        List<String> businessRules();
    }
    ```

4.  **File Location (Concrete Blueprint):** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/blueprint/spec/StandardPaymentBlueprint.java`
5.  **Visibility:** `public`
6.  **Action:** Create a concrete blueprint for a standard payment flow. It uses nested interfaces to create a clean, hierarchical contract. The method signatures (name and return type) *are* the contract.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.blueprint.spec;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import dexter.banking.booktransfers.core.port.in.SubmitPaymentCommand;
    import dexter.banking.booktransfers.core.port.out.CreditPort;
    import dexter.banking.booktransfers.core.port.out.DepositPort;
    import dexter.banking.booktransfers.core.port.out.LimitPort;
    import dexter.banking.statemachine.StateMachineFactory;

    public interface StandardPaymentBlueprint extends JourneyBlueprint {
        AdapterRoutingBlueprint adapterRouting();
        OrchestrationBlueprint orchestration();

        interface AdapterRoutingBlueprint {
            DepositPort depositPort();
            CreditPort creditPort();
            LimitPort limitPort();
        }

        interface OrchestrationBlueprint {
            StateMachineFactory<SubmitPaymentCommand> engine();
        }
    }
    ```

#### Sub-step 1.3: Define the `application.yml` Contract

The YAML structure is a direct mapping to the blueprint interfaces. The journey key (e.g., `PAYMENT_SUBMIT_V1`) is the identifier that will be returned by `Command.getIdentifier()`.

```yaml
app:
  journeys:
    PAYMENT_SUBMIT_V1:
      journeyName: "PAYMENT_SUBMIT_V1"
      journeyType: "STANDARD_PAYMENT"
      validationGroups: ["STANDARD_PAYMENT", "WALLET_JOURNEY"]
      orchestration:
        engine: "standardPaymentStateMachineFactory"
      adapterRouting:
        depositPort: "DEPOSIT_PORT_REST"
        creditPort: "CREDIT_PORT_JMS"
        limitPort: "LIMIT_PORT_REST"
      dataCollectors: ["customerProfileCollector", "accountBalanceCollector"]
      businessRules: ["sufficientFundsRule", "customerStatusRule"]
```

---

## 3. Step 2: The Startup-Time Guarantee: Blueprint Materializer

### The "Why"

The application shall not start if it is configured in a state that guarantees runtime failure. This validation must be automatic, generic, and derived from the type-safe blueprints.

### The "What"

We will implement a **"Blueprint Materializer"**. This is a Spring `@Configuration` component that runs at startup. Its job is to proactively load every journey definition from the YAML and attempt to build a fully-materialized, type-safe `JourneyBlueprint` proxy for it. The act of successfully creating this proxy *is* the validation. This component replaces the old `ConfigurationPort` and provides a map of the final, immutable, and fully-validated `JourneySpecification` objects to the rest of the framework.

### The "How"

This component is pure infrastructure and will reside in the `infrastructure` module.

#### Sub-step 2.1: Implement the `BlueprintConfiguration`

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/config/BlueprintConfiguration.java`
2.  **Visibility:** `public`
3.  **Action:** Create the configuration class that performs the materialization.

    ```java
    package dexter.banking.booktransfers.infrastructure.config;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import dexter.banking.booktransfers.infrastructure.provider.BlueprintProxyFactory;
    import org.springframework.boot.context.properties.ConfigurationProperties;
    import org.springframework.context.ApplicationContext;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import java.util.Map;
    import java.util.stream.Collectors;

    @Configuration
    public class BlueprintConfiguration {

        @Bean
        @ConfigurationProperties(prefix = "app")
        public JourneysConfigurationProperties journeysConfigurationProperties() {
            return new JourneysConfigurationProperties();
        }

        @Bean
        public Map<String, JourneySpecification> journeySpecifications(
            ApplicationContext applicationContext,
            JourneysConfigurationProperties journeysConfig
        ) {
            return journeysConfig.getJourneys().entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> {
                    String journeyName = entry.getKey();
                    Map<String, Object> rawConfig = entry.getValue();

                    try {
                        JourneyType journeyType = JourneyType.valueOf((String) rawConfig.get("journeyType"));
                        Class<? extends JourneyBlueprint> blueprintClass = journeyType.getBlueprintClass();

                        JourneyBlueprint blueprintProxy = BlueprintProxyFactory.createProxy(
                            blueprintClass,
                            rawConfig,
                            applicationContext
                        );

                        return new JourneySpecification(journeyName, journeyType, rawConfig, blueprintProxy);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to materialize blueprint for journey: '" + journeyName + "'", e);
                    }
                }
            ));
        }

        // Helper class for strong-typing the YAML structure
        public static class JourneysConfigurationProperties {
            private final Map<String, Map<String, Object>> journeys = new java.util.HashMap<>();
            public Map<String, Map<String, Object>> getJourneys() { return journeys; }
        }
    }
    ```

#### Sub-step 2.2: Implement the `BlueprintProxyFactory`

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/blueprint/BlueprintProxyFactory.java`
2.  **Visibility:** `public`
3.  **Action:** Create the factory that builds the dynamic proxies using `java.lang.reflect.Proxy`.

    ```java
    package dexter.banking.booktransfers.infrastructure.blueprint;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import org.springframework.context.ApplicationContext;
    import java.lang.reflect.InvocationHandler;
    import java.lang.reflect.Method;
    import java.lang.reflect.Proxy;
    import java.util.Map;

    public final class BlueprintProxyFactory {

        private BlueprintProxyFactory() {}

        @SuppressWarnings("unchecked")
        public static <T extends JourneyBlueprint> T createProxy(
            Class<T> blueprintInterface,
            Map<String, Object> config,
            ApplicationContext ctx
        ) {
            return (T) Proxy.newProxyInstance(
                blueprintInterface.getClassLoader(),
                new Class<?>[]{blueprintInterface},
                new BlueprintInvocationHandler(config, ctx)
            );
        }

        private static class BlueprintInvocationHandler implements InvocationHandler {
            private final Map<String, Object> config;
            private final ApplicationContext ctx;

            public BlueprintInvocationHandler(Map<String, Object> config, ApplicationContext ctx) {
                this.config = config;
                this.ctx = ctx;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String key = method.getName();
                Object value = config.get(key);
                Class<?> returnType = method.getReturnType();

                if (value == null) {
                    throw new IllegalStateException(String.format(
                        "Configuration key '%s' not found in journey config for blueprint '%s'",
                        key, method.getDeclaringClass().getSimpleName()
                    ));
                }

                if (JourneyBlueprint.class.isAssignableFrom(returnType)) {
                    return createProxy(
                        (Class<? extends JourneyBlueprint>) returnType,
                        (Map<String, Object>) value,
                        ctx
                    );
                } else if (isSpringBean(returnType, value)) {
                    String beanName = (String) value;
                    if (!ctx.containsBean(beanName)) {
                         throw new IllegalStateException(String.format(
                            "Configuration error: Bean with name '%s' referenced by key '%s' does not exist.",
                            beanName, key
                         ));
                    }
                    return ctx.getBean(beanName, returnType);
                } else {
                    return value;
                }
            }

            private boolean isSpringBean(Class<?> returnType, Object value) {
                if (!(value instanceof String)) return false;
                // Check if the application context has any beans of the given return type.
                // This is a heuristic to distinguish between simple string properties and bean references.
                return ctx.getBeanNamesForType(returnType).length > 0;
            }
        }
    }
    ```

---

## 4. Step 3: Secure & Agnostic Context Propagation

### The "Why"

Our architecture is critically dependent on the journey context being available to any component that needs it, regardless of the entry point (HTTP, Kafka, Scheduler). The context must be established at the most generic entry points to the core: the Command and Query buses.

### The "What"

We will implement a unified, technology-agnostic context injection framework. This consists of a single, secure way to **access** the context, and a minimal set of ways to **establish** it.

1.  **`@InJourney` Annotation (Access):** The *only* sanctioned way to access the context.
2.  **`JourneyContextManager` (Mechanism):** The existing, correct utility for managing the `ScopedValue` lifecycle.
3.  **`InJourneyParameterAspect` (Mechanism):** A lightweight, compile-time woven aspect that injects the blueprint from the `JourneyContextManager`.
4.  **`ConfigurationEnrichmentMiddleware` (Establishment):** The refactored, mandatory middleware that wraps the `CommandBus`.
5.  **`@WithJourneyContext` Annotation (Establishment):** A declarative way to establish context for non-command entry points.

### The "How"

#### Sub-step 3.1: Refactor the `ConfigurationEnrichmentMiddleware`

This middleware is the primary, technology-agnostic entry point for establishing context for all commands. We will refactor it to use the materialized blueprints from Step 2.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/application/middleware/ConfigurationEnrichmentMiddleware.java`
2.  **Visibility:** `public`
3.  **Action:** Modify the middleware to use the `journeySpecifications` map. The `ConfigurationPort` is now obsolete.

    ```java
    package dexter.banking.booktransfers.core.application.middleware;

    import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextDeprecated;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManagerDeprecated;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import dexter.banking.commandbus.Command;
    import dexter.banking.commandbus.Middleware;
    import lombok.RequiredArgsConstructor;
    import lombok.extern.slf4j.Slf4j;
    import org.springframework.core.annotation.Order;
    import org.springframework.stereotype.Component;
    import java.util.Map;

    @Order(1)
    @Component
    @RequiredArgsConstructor
    @Slf4j
    public class ConfigurationEnrichmentMiddleware implements Middleware {

        // Inject the map of pre-materialized, validated specifications from Step 2.
        private final Map<String, JourneySpecification> journeySpecifications;

        @Override
        public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
            // The command identifier IS the journey name.
            String journeyName = command.getIdentifier();
            JourneySpecification spec = journeySpecifications.get(journeyName);

            if (spec == null) {
                // Fail hard if no configuration is found for a given command.
                throw new IllegalStateException("No journey specification found for command identifier: " + journeyName);
            }

            // The JourneyContext now holds our rich, validated JourneySpecification object.
            var context = new JourneyContext(spec);

            log.debug("Running command for journey '{}' with ScopedValue journey context", journeyName);

            try {
                return JourneyContextManager.runWithContext(context, next::invoke);
            } catch (Exception e) {
                // Wrap and rethrow to avoid checked exceptions in the middleware signature.
                throw new RuntimeException(e);
            }
        }
    }
    ```

#### Sub-step 3.2: Implement the Ad-Hoc Context Annotator

This provides a flexible entry point for queries and other non-command scenarios.

1.  **Define `@WithJourneyContext`:**
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/context/WithJourneyContext.java`
    *   **Visibility:** `public`
    *   **Code:**
        ```java
        package dexter.banking.booktransfers.core.domain.shared.context;

        import java.lang.annotation.ElementType;
        import java.lang.annotation.Retention;
        import java.lang.annotation.RetentionPolicy;
        import java.lang.annotation.Target;

        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface WithJourneyContext {
            /**
             * A SpEL expression to extract the journey identifier from the method arguments.
             * Example: "#query.journeyId" or "#request.headers['X-Journey-Name']"
             */
            String journeyIdentifier();
        }
        ```

2.  **Implement `WithJourneyContextAspect`:**
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/aspect/WithJourneyContextAspect.java`
    *   **Visibility:** `public`
    *   **Action:** Create a Spring `@Component` with an `@Around` advice. This is a complete implementation.
        ```java
        package dexter.banking.booktransfers.infrastructure.aspect;

        import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextDeprecated;
        import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManagerDeprecated;
        import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
        import dexter.banking.booktransfers.core.domain.shared.context.WithJourneyContext;
        import org.aspectj.lang.ProceedingJoinPoint;
        import org.aspectj.lang.annotation.Around;
        import org.aspectj.lang.annotation.Aspect;
        import org.aspectj.lang.reflect.MethodSignature;
        import org.springframework.context.expression.MethodBasedEvaluationContext;
        import org.springframework.core.DefaultParameterNameDiscoverer;
        import org.springframework.core.ParameterNameDiscoverer;
        import org.springframework.expression.spel.standard.SpelExpressionParser;
        import org.springframework.stereotype.Component;
        import java.lang.reflect.Method;
        import java.util.Map;

        @Aspect
        @Component
        public class WithJourneyContextAspect {

            private final Map<String, JourneySpecification> journeySpecifications;
            private final SpelExpressionParser expressionParser = new SpelExpressionParser();
            private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

            public WithJourneyContextAspect(Map<String, JourneySpecification> journeySpecifications) {
                this.journeySpecifications = journeySpecifications;
            }

            @Around("@annotation(withJourneyContextDeprecated)")
            public Object establishAdHocContext(ProceedingJoinPoint pjp, WithJourneyContext withJourneyContextDeprecated) throws Throwable {
                // 1. Parse SpEL expression to get journey identifier
                Method method = ((MethodSignature) pjp.getSignature()).getMethod();
                MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(pjp.getTarget(), method, pjp.getArgs(), parameterNameDiscoverer);
                String journeyName = (String) expressionParser.parseExpression(withJourneyContextDeprecated.journeyIdentifier()).getValue(evaluationContext);

                if (journeyName == null) {
                    throw new IllegalStateException("SpEL expression for @WithJourneyContext resolved to null.");
                }

                // 2. Fetch the specification
                JourneySpecification spec = journeySpecifications.get(journeyName);
                if (spec == null) {
                    throw new IllegalStateException("No journey specification found for identifier: " + journeyName);
                }

                // 3. Wrap the call in the context
                var context = new JourneyContext(spec);
                try {
                    return JourneyContextManager.runWithContext(context, pjp::proceed);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        ```

#### Sub-step 3.3: The Secure Access Pattern (`@InJourney`)

This pattern provides the single, secure way for application code to receive the journey blueprint.

1.  **`@InJourney` Annotation:** A simple `PARAMETER`-level marker annotation.
2.  **`InJourneyParameterAspect`:** A stateless, compile-time woven aspect. Its only job is to retrieve the pre-built blueprint from the established context (`JourneyContextManager.getContext().specification().getBlueprint()`), verify its type against the method parameter, and inject it. This aspect is **not** a Spring Component. Its implementation is critical and must be complete.

    ```java
    package dexter.banking.booktransfers.infrastructure.aspect;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import dexter.banking.booktransfers.core.domain.shared.context.InJourney;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManagerDeprecated;
    import org.aspectj.lang.ProceedingJoinPoint;
    import org.aspectj.lang.annotation.Around;
    import org.aspectj.lang.annotation.Aspect;
    import org.aspectj.lang.reflect.MethodSignature;
    import java.lang.reflect.Parameter;

    @Aspect
    public class InJourneyParameterAspect {

        @Around("execution(* *(.., @dexter.banking.booktransfers.core.domain.shared.context.InJourney (*), ..))")
        public Object injectJourneyBlueprint(ProceedingJoinPoint pjp) throws Throwable {
            JourneyBlueprint prebuiltBlueprint = JourneyContextManager.getContext().specification().getBlueprint();

            MethodSignature signature = (MethodSignature) pjp.getSignature();
            int injectionIndex = -1;
            Class<?> requestedBlueprintType = null;
            Parameter[] parameters = signature.getMethod().getParameters();
            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(InJourney.class)) {
                    injectionIndex = i;
                    requestedBlueprintType = parameters[i].getType();
                    break;
                }
            }

            if (requestedBlueprintType == null || !requestedBlueprintType.isInstance(prebuiltBlueprint)) {
                throw new ClassCastException(String.format(
                    "Cannot inject blueprint of type '%s' into parameter of type '%s' for method '%s'.",
                    prebuiltBlueprint.getClass().getInterfaces()[0].getSimpleName(),
                    requestedBlueprintType != null ? requestedBlueprintType.getSimpleName() : "unknown",
                    signature.getMethod().getName()
                ));
            }

            Object[] args = pjp.getArgs();
            args[injectionIndex] = prebuiltBlueprint;
            return pjp.proceed(args);
        }
    }
    ```
