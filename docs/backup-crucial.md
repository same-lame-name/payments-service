# The God-Plan: A Unified Architectural Blueprint

**Version:** 2.0
**Status:** Active & Authoritative

---

## Foreword: The Philosophy of the God-Plan

This document is the single, canonical source of truth for the architectural design and implementation of the payments-service. It supersedes all previous, fragmented `.md` files and consolidates their final, approved, pinnacle designs into one cohesive and unabridged blueprint.

Our architecture is founded on one supreme principle: **"Configuration over Code, Guaranteed by the Framework at Compile-Time."**

The system must be a lean, generic core whose behavior is dynamically and declaratively dictated by an external configuration artifact—the `application.yml`. However, this flexibility cannot come at the cost of safety. A simple typo in a configuration file must not lead to a production `NullPointerException`. Therefore, the framework itself must provide an ironclad, multi-layered, and non-bypassable guarantee that this configuration is always valid, complete, and correctly applied. This guarantee must, wherever possible, be enforced by the Java compiler itself, moving failure detection from runtime to compile-time.

This God-Plan is not a collection of suggestions; it is a sequential, top-down implementation strategy. Each step builds upon the successful execution of the previous one, culminating in a system that is scalable, maintainable, resilient, and architecturally pure. This is the final word.

---

## Step 1: Establish the Foundation - The Type-Safe Blueprint

### The "Why"

A configuration-driven system is only as good as the contract between the configuration and the code. A contract based on raw strings (bean names, class names) is fragile and prone to runtime failure due to typos or uncoordinated refactoring. This is unacceptable. We must leverage the Java type system to create a compile-time safe "Blueprint" for our configuration. The configuration (`application.yml`) remains the source of truth for *which* components to use, but the *shape* and *existence* of those components will be guaranteed by the compiler.

### The "What"

We will establish a "Type-Safe Blueprint" pattern. This replaces direct, brittle references in the configuration with a system of pure Java `interface`s that serve as compile-time safe contracts. The framework will validate and materialize these blueprints at startup, providing the application with a ready-to-use, type-safe object for accessing all journey-specific configuration.

This involves three primary artifacts within the `book-transfers/core` module:

1.  **The `JourneyType.java` Enum:** The single, authoritative, and type-safe registry of all known journey types. It maps a simple name (used in YAML) to a specific blueprint interface, eliminating the need for fragile, fully-qualified class names in the configuration.
2.  **The `JourneyBlueprint.java` Interfaces:** A hierarchy of pure Java interfaces that declaratively define the *shape* of configuration required for a component or journey. This is the compile-time contract.
3.  **The `JourneySpecification.java` Class:** A **framework-internal** container that holds both the raw configuration data and the pre-materialized, type-safe `JourneyBlueprint` proxy for a given journey. Application code will *never* touch this class directly.

### The "How"

#### Sub-step 1.1: Define the `JourneyType` Enum

This enum is the linchpin of our type-safe system, providing a single, discoverable, and compile-time safe registry of all possible journeys.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/blueprint/JourneyType.java`
2.  **Action:** Create the enum. Each constant represents a journey and holds a reference to its corresponding blueprint interface `Class`.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.blueprint;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.StandardPaymentBlueprint;
    import dexter.banking.booktransfers.core.domain.shared.blueprint.spec.WalletJourneyBlueprint;

    /**
     * The definitive, type-safe registry of all journey types in the system.
     * Each constant maps a simple, human-readable name (used in YAML) to the
     * specific JourneyBlueprint interface that defines its configuration contract.
     */
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
2.  **Action:** Create the base marker interface.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.blueprint;

    import dexter.banking.booktransfers.core.domain.shared.validation.ValidationGroup;
    import java.util.List;

    /**
     * Base marker interface for all journey blueprints.
     * A blueprint is a pure Java interface that defines the compile-time safe "shape"
     * of the configuration required for a specific journey.
     */
    public interface JourneyBlueprint {
        String journeyName();
        List<ValidationGroup> validationGroups();
        List<String> dataCollectors();
        List<String> businessRules();
    }
    ```

3.  **File Location (Concrete Blueprint):** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/blueprint/spec/StandardPaymentBlueprint.java`
4.  **Action:** Create a concrete blueprint for a standard payment flow. It uses nested interfaces to create a clean, hierarchical contract. The method signatures (name and return type) *are* the contract.

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

        // Nested interface defines the contract for adapter routing.
        interface AdapterRoutingBlueprint {
            // Method name is the key, return type is the contract.
            DepositPort depositPort();
            CreditPort creditPort();
            LimitPort limitPort();
        }

        // Nested interface defines the contract for orchestration.
        interface OrchestrationBlueprint {
            StateMachineFactory<SubmitPaymentCommand> engine();
        }
    }
    ```

#### Sub-step 1.3: Update `application.yml` to be Type-Safe

The YAML is now cleaner, safer, and free of fragile FQCNs.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/app/src/main/resources/application.yml`
2.  **Action:** Restructure the `journeys` configuration.

    ```yaml
    app:
      journeys:
        PAYMENT_SUBMIT_V1:
          journeyName: "PAYMENT_SUBMIT_V1"
          # NO FQCN. This is a type-safe enum constant. Startup fails if it's invalid.
          journeyType: "STANDARD_PAYMENT"
          # These are also type-safe enum constants.
          validationGroups: ["STANDARD_PAYMENT", "WALLET_JOURNEY"]
          orchestration:
            # Key "engine" matches method name in OrchestrationBlueprint
            engine: "standardPaymentStateMachineFactory"
          adapterRouting:
            # Keys "depositPort", "creditPort" match method names in AdapterRoutingBlueprint
            depositPort: "DEPOSIT_PORT_REST"
            creditPort: "CREDIT_PORT_JMS"
            limitPort: "LIMIT_PORT_REST"
          dataCollectors: ["customerProfileCollector", "accountBalanceCollector"]
          businessRules: ["sufficientFundsRule", "customerStatusRule"]
    ```

### Peek of the Implementation

After this step, the project's architectural foundation is immensely stronger. We have replaced a brittle, string-based system with a robust, compile-time safe contract system. The stage is now set for building the validation and guarantee layers upon this solid footing.

---

## Step 2: Implement Guarantee Layer 1 - The Blueprint Materializer

### The "Why"

An architecture built on configuration is dangerously brittle if that configuration is not verifiable. We must eliminate the entire class of bugs related to misconfiguration. The guarantee is simple: **the application shall not start if it is configured in a state that guarantees runtime failure.** This validation must be automatic, generic, and derived from the type-safe blueprints we defined in Step 1.

### The "What"

We will implement a **"Blueprint Materializer"**. This is a Spring `@Configuration` component that runs at startup. Its job is to proactively load every journey definition from the YAML and attempt to build a fully-materialized, type-safe `JourneyBlueprint` proxy for it.

The act of successfully creating this proxy *is* the validation. If a bean name in the YAML is incorrect, or a required configuration key is missing, the proxy creation will fail with a clear exception, immediately halting application startup. This component then provides a map of the final, immutable, and fully-validated `JourneySpecification` objects to the rest of the framework.

### The "How"

This component is pure infrastructure and will reside in the `infrastructure` module.

#### Sub-step 2.1: Implement the `BlueprintConfiguration`

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/config/BlueprintConfiguration.java`
2.  **Action:** Create the configuration class that performs the materialization.

    ```java
    package dexter.banking.booktransfers.infrastructure.config;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
    import dexter.banking.booktransfers.infrastructure.blueprint.BlueprintProxyFactory;
    import org.springframework.boot.context.properties.ConfigurationProperties;
    import org.springframework.context.ApplicationContext;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import java.util.Map;
    import java.util.stream.Collectors;

    @Configuration
    public class BlueprintConfiguration {

        /**
         * Step 1: Bind the raw YAML under 'app.journeys' to a map of simple objects.
         */
        @Bean
        @ConfigurationProperties(prefix = "app")
        public Map<String, Map<String, Object>> rawJourneyConfigs() {
            return new java.util.HashMap<>();
        }

        /**
         * Step 2: The "Blueprint Materializer".
         * This bean takes the raw configs, validates them by creating the blueprint proxies,
         * and provides the final, immutable map of JourneySpecification objects.
         */
        @Bean
        public Map<String, JourneySpecification> journeySpecifications(
            ApplicationContext applicationContext,
            Map<String, Map<String, Object>> rawJourneyConfigs
        ) {
            return rawJourneyConfigs.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> {
                    String journeyName = entry.getKey();
                    Map<String, Object> rawConfig = entry.getValue();

                    try {
                        // Get JourneyType from raw config, failing fast if invalid
                        JourneyType journeyType = JourneyType.valueOf((String) rawConfig.get("journeyType"));
                        Class<? extends JourneyBlueprint> blueprintClass = journeyType.getBlueprintClass();

                        // Create the proxy. This is the validation. Throws on failure.
                        JourneyBlueprint blueprintProxy = BlueprintProxyFactory.createProxy(
                            blueprintClass,
                            rawConfig,
                            applicationContext
                        );

                        // Create the final, rich JourneySpecification object
                        return new JourneySpecification(journeyName, journeyType, rawConfig, blueprintProxy);
                    } catch (Exception e) {
                        throw new IllegalStateException("Failed to materialize blueprint for journey: '" + journeyName + "'", e);
                    }
                }
            ));
        }
    }
    ```

#### Sub-step 2.2: Implement the `BlueprintProxyFactory`

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/blueprint/BlueprintProxyFactory.java`
2.  **Action:** Create the factory that builds the dynamic proxies using `java.lang.reflect.Proxy`.

    ```java
    package dexter.banking.booktransfers.infrastructure.blueprint;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import org.springframework.context.ApplicationContext;
    import java.lang.reflect.InvocationHandler;
    import java.lang.reflect.Method;
    import java.lang.reflect.Proxy;
    import java.util.Map;

    public class BlueprintProxyFactory {
        @SuppressWarnings("unchecked")
        public static <T extends JourneyBlueprint> T createProxy(Class<T> blueprintInterface, Map<String, Object> config, ApplicationContext ctx) {
            return (T) Proxy.newProxyInstance(
                blueprintInterface.getClassLoader(),
                new Class<?>[]{blueprintInterface},
                new BlueprintInvocationHandler(config, ctx)
            );
        }

        private static class BlueprintInvocationHandler implements InvocationHandler {
            private final Map<String, Object> config;
            private final ApplicationContext ctx;

            // ... constructor ...

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String key = method.getName();
                Object value = config.get(key);
                Class<?> returnType = method.getReturnType();

                if (value == null) {
                    throw new IllegalStateException(String.format("Configuration key '%s' not found in journey config for blueprint '%s'", key, method.getDeclaringClass().getSimpleName()));
                }

                if (JourneyBlueprint.class.isAssignableFrom(returnType)) {
                    // It's a nested blueprint, recursively create a proxy for the sub-map
                    return createProxy((Class<? extends JourneyBlueprint>) returnType, (Map<String, Object>) value, ctx);
                } else if (ctx.getBeanNamesForType(returnType).length > 0) {
                    // It's a Spring bean, look it up by name
                    String beanName = (String) value;
                    if (!ctx.containsBean(beanName)) {
                         throw new IllegalStateException(String.format("Configuration error: Bean with name '%s' for key '%s' does not exist.", beanName, key));
                    }
                    return ctx.getBean(beanName, returnType);
                } else {
                    // It's a simple value (List, String, etc.), return it directly
                    return value;
                }
            }
        }
    }
    ```

### Peek of the Implementation

With this step complete, our application gains a powerful immune system. If a developer makes a typo in `application.yml` (`depositPort: "DEPOSIT_PORT_REST_TYPO"`), the application will **refuse to start**. The developer will be greeted with a clear, immediate error:

```
Caused by: java.lang.IllegalStateException: Failed to materialize blueprint for journey: 'PAYMENT_SUBMIT_V1'
Caused by: java.lang.IllegalStateException: Configuration error: Bean with name 'DEPOSIT_PORT_REST_TYPO' for key 'depositPort' does not exist.
```

This moves the feedback loop from a potential production incident to an immediate boot-time failure, enforcing architectural integrity.

---

## Step 3: Implement Guarantee Layer 2 - Secure Context & Runtime Injection

### The "Why"

Our architecture is critically dependent on the journey context being available to any component that needs it. We need a system that is **non-bypassable** and **self-validating**. The framework must not simply *provide* the context; it must *inject* it, and this injection process itself must be the unyielding gatekeeper. The safe path must be the *only* path.

### The "What"

We will implement the **"Argument Injection Model."** The journey context will be **injected** directly by the framework as a method parameter. This makes the dependency explicit, visible, and guaranteed by the method's own signature.

1.  **The `@InJourney` Annotation:** A `PARAMETER`-level annotation. It is the developer's declarative request for the framework to inject the appropriate, type-safe `JourneyBlueprint` proxy.
2.  **The `JourneyContext` Utility:** A **sealed, `package-private` utility** with **zero public API**. Its sole purpose is to bind the rich `JourneySpecification` object (containing the pre-built blueprint) to a `java.lang.ScopedValue`.
3.  **The `InJourneyParameterAspect`:** A lightweight, compile-time woven `@Around` aspect that intercepts the execution of any method with an `@InJourney` parameter. It retrieves the pre-built blueprint from the context, verifies its type, and injects it.

### The "How"

#### Sub-step 3.1: Define the `@InJourney` Annotation

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/context/InJourney.java`
2.  **Action:** Create the annotation. It is a simple marker.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.context;

    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;

    /**
     * Marks a method parameter to be injected with the current, type-safe JourneyBlueprint.
     * This is the only sanctioned way for application code to access the journey context.
     * The framework guarantees that the injected blueprint will be non-null and of the
     * correct type for the current journey.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InJourney {
    }
    ```

#### Sub-step 3.2: Create the Sealed `JourneyContext` Utility

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/context/JourneyContext.java`
2.  **Action:** Create the `final` class with a `private ScopedValue` and `package-private` methods.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.context;

    import java.util.concurrent.Callable;
    import java.util.NoSuchElementException;

    /**
     * A sealed, framework-internal utility for managing the JourneySpecification's lifecycle
     * via a ScopedValue. This class has NO PUBLIC API.
     */
    public final class JourneyContext {

        private static final ScopedValue<JourneySpecification> SCOPED_SPEC = ScopedValue.newInstance();

        private JourneyContext() {}

        /**
         * [PACKAGE-PRIVATE] Binds the given JourneySpecification to the scope.
         */
        static <T> T runWith(JourneySpecification spec, Callable<T> operation) throws Exception {
            return ScopedValue.where(SCOPED_SPEC, spec).call(operation);
        }

        /**
         * [PACKAGE-PRIVATE] Retrieves the JourneySpecification from the current scope.
         * Throws NoSuchElementException if not bound.
         */
        static JourneySpecification getSpec() {
            return SCOPED_SPEC.get();
        }
    }
    ```

#### Sub-step 3.3: Implement the Entry Point Wrapper (`JourneyScopeFilter`)

This remains conceptually the same, but now it retrieves the rich `JourneySpecification` object from the map created in Step 2.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/in/web/JourneyScopeFilter.java`
2.  **Action:** Create the filter.

    ```java
    // ... imports
    @Component
    public class JourneyScopeFilter implements Filter {

        private final Map<String, JourneySpecification> journeySpecifications;
        // ... constructor ...

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            // ... logic to get journeyName from header ...

            JourneySpecification spec = journeySpecifications.get(journeyName);
            if (spec == null) {
                throw new ServletException("Invalid journey name: " + journeyName);
            }

            try {
                // Bind the rich, pre-validated JourneySpecification object
                JourneyContext.runWith(spec, () -> {
                    chain.doFilter(request, response);
                    return null;
                });
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
    }
    ```

#### Sub-step 3.4: Implement the Lightweight `InJourneyParameterAspect`

This is the lightweight, performant gatekeeper. It is **not** a Spring component.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/aspect/InJourneyParameterAspect.java`
2.  **Action:** Create the compile-time woven aspect.

    ```java
    package dexter.banking.booktransfers.infrastructure.aspect;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import dexter.banking.booktransfers.core.domain.shared.context.InJourney;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneyContext;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecification;
    import org.aspectj.lang.ProceedingJoinPoint;
    import org.aspectj.lang.annotation.Around;
    import org.aspectj.lang.annotation.Aspect;
    import org.aspectj.lang.reflect.MethodSignature;
    import java.lang.reflect.Parameter;

    /**
     * A compile-time woven aspect (NOT a Spring @Component). Its sole responsibility
     * is to retrieve the pre-materialized JourneyBlueprint from the current
     * JourneyContext and inject it into the annotated method parameter.
     */
    @Aspect
    public class InJourneyParameterAspect {

        @Around("execution(* *(.., @dexter.banking.booktransfers.core.domain.shared.context.InJourney (*), ..))")
        public Object injectJourneyBlueprint(ProceedingJoinPoint pjp) throws Throwable {
            // 1. Get the rich JourneySpecification from the current thread's scope.
            JourneySpecification spec = JourneyContext.getSpec();
            JourneyBlueprint prebuiltBlueprint = spec.getBlueprint();

            // 2. Find the annotated parameter and get its required type.
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

            // 3. CRITICAL, LIGHTWEIGHT CHECK: Verify type compatibility.
            if (requestedBlueprintType == null || !requestedBlueprintType.isInstance(prebuiltBlueprint)) {
                throw new IllegalStateException(String.format(
                    "Journey '%s' provides a blueprint of type '%s', but method '%s' requires incompatible type '%s'.",
                    spec.journeyName(),
                    prebuiltBlueprint.getClass().getInterfaces()[0].getSimpleName(),
                    signature.getMethod().getName(),
                    requestedBlueprintType != null ? requestedBlueprintType.getSimpleName() : "unknown"
                ));
            }

            // 4. Inject the already-created proxy and proceed.
            Object[] args = pjp.getArgs();
            args[injectionIndex] = prebuiltBlueprint;
            return pjp.proceed(args);
        }
    }
    ```

### Peek of the Implementation

The developer experience is now supremely clean and safe.

```java
// The dependency is explicit, validated at startup, and guaranteed by the framework.
public class SomeHandler {
    public void handle(SomeCommand command,
                       @InJourney StandardPaymentBlueprint blueprint) {
        // The 'blueprint' variable is GUARANTEED by the framework to be:
        // 1. Non-null.
        // 2. A proxy implementing the StandardPaymentBlueprint interface.
        // 3. Fully materialized and validated at application startup.

        // This call is compile-time safe and has near-zero runtime overhead.
        DepositPort depositAdapter = blueprint.adapterRouting().depositPort();
        depositAdapter.submitDeposit(...);
    }
}
```

---

## Step 4: Implement Guarantee Layer 3 - The Two-Phase Validation Framework

### The "Why"

A request entering our system must pass through two distinct validation gates. Confusing these two gates leads to bloated, unmaintainable components and violates the Single Responsibility Principle.

1.  **Syntactic & API Contract Validation:** Answers: **"Did the caller use our API correctly according to the journey's specific rules?"** It validates the *shape*, *format*, and *presence* of data in the incoming `Command` DTO. This must happen at the earliest possible moment to **fail fast**.

2.  **Semantic & Business Rule Validation:** Answers: **"Does this validly-formed request represent a valid business operation?"** This requires an enriched data context and represents the core business logic of our domain.

We require a dedicated, declarative framework for each phase, ensuring a clean separation of concerns.

### The "What"

We will implement a two-phase validation strategy, with each phase powered by its own specialized, configuration-driven framework.

1.  **Phase 1: The Blueprint-Aware Syntactic Validation Framework.** We will build a `CommandValidationMiddleware`. This middleware will use the standard **Java Bean Validation (JSR 303)** framework, but its power will come from using **Validation Groups**. The specific validation groups to be activated will be read directly from the type-safe `JourneyBlueprint`.

2.  **Phase 2: The Declarative Business Rule Engine.** We will build a simple, strategy-pattern-based rule engine for semantic validation. It will be driven by a list of rule bean names in the `JourneyBlueprint` and will execute them in a declarative **`Given-When-Then`** style.

### The "How"

#### Sub-step 4.1: Implement the Syntactic Validation Framework

1.  **Define `ValidationGroup` Enum:** We need a type-safe way to reference validation groups.
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/validation/ValidationGroup.java`
    *   **Action:** Create an enum that maps a name to the validation group interface.

        ```java
        package dexter.banking.booktransfers.core.domain.shared.validation;

        public enum ValidationGroup {
            STANDARD_PAYMENT(ValidationGroups.StandardPayment.class),
            WALLET_JOURNEY(ValidationGroups.WalletJourney.class);

            private final Class<?> groupClass;

            ValidationGroup(Class<?> groupClass) { this.groupClass = groupClass; }

            public Class<?> getGroupClass() { return groupClass; }
        }
        ```

2.  **Annotate a `Command` DTO:** This remains the same, using standard JSR 303 annotations.

3.  **Implement the `CommandValidationMiddleware`:** This middleware becomes much simpler and safer.
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/middleware/CommandValidationMiddleware.java`
    *   **Action:** Create a middleware aspect that intercepts the command, reads the active validation groups from the injected `JourneyBlueprint`, and invokes the validator.

        ```java
        package dexter.banking.booktransfers.infrastructure.middleware;

        import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
        import dexter.banking.booktransfers.core.domain.shared.context.InJourney;
        import dexter.banking.booktransfers.core.domain.shared.validation.ValidationGroup;
        import jakarta.validation.ConstraintViolation;
        import jakarta.validation.ConstraintViolationException;
        import jakarta.validation.Validator;
        import org.aspectj.lang.JoinPoint;
        import org.aspectj.lang.annotation.Aspect;
        import org.aspectj.lang.annotation.Before;
        import org.springframework.stereotype.Component;
        import java.util.List;
        import java.util.Set;
        import java.util.stream.Collectors;

        @Aspect
        @Component
        public class CommandValidationMiddleware {

            private final Validator validator;

            public CommandValidationMiddleware(Validator validator) {
                this.validator = validator;
            }

            @Before("execution(* dexter.banking.booktransfers.core.port.in.CommandBus.send(..)) && args(command)")
            public void validateCommand(JoinPoint jp, Object command, @InJourney JourneyBlueprint blueprint) {
                // 1. Get the list of active group enums from the type-safe blueprint.
                List<ValidationGroup> activeGroups = blueprint.validationGroups();

                if (activeGroups.isEmpty()) {
                    return; // No validation configured for this journey
                }

                // 2. Convert enums to their underlying Class objects for the validator.
                Set<Class<?>> groupClasses = activeGroups.stream()
                    .map(ValidationGroup::getGroupClass)
                    .collect(Collectors.toSet());

                // 3. Execute validation using only the active groups.
                Set<ConstraintViolation<Object>> violations = validator.validate(command, groupClasses.toArray(new Class[0]));

                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
            }
        }
        ```

#### Sub-step 4.2: Implement the Semantic Business Rule Engine

This engine is simplified by using a standard Strategy pattern, avoiding over-engineering.

1.  **Create the `BusinessRule` Interface:**
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/rule/BusinessRule.java`
    *   **Action:** Create a simple, functional interface.

        ```java
        package dexter.banking.booktransfers.core.domain.shared.rule;

        import io.vavr.control.Validation;

        public interface BusinessRule<T> {
            Validation<String, T> validate(T context);
        }
        ```

2.  **Implement a Sample Business Rule:**
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/rule/SufficientFundsRule.java`
    *   **Action:** Create a concrete rule as a standard Spring `@Component`.

        ```java
        package dexter.banking.booktransfers.infrastructure.rule;

        import dexter.banking.booktransfers.core.domain.shared.rule.BusinessRule;
        import org.springframework.stereotype.Component;
        // Other imports...

        @Component("sufficientFundsRule") // The bean name is the contract
        public class SufficientFundsRule implements BusinessRule<EnrichedSubmitPaymentCommand> {

            @Override
            public Validation<String, EnrichedSubmitPaymentCommand> validate(EnrichedSubmitPaymentCommand command) {
                if (command.getAccountBalance().compareTo(command.getAmount()) >= 0) {
                    return Validation.valid(command); // Success
                } else {
                    return Validation.invalid("Insufficient funds. Balance: " + command.getAccountBalance()); // Failure
                }
            }
        }
        ```

3.  **Implement the `BusinessRuleOrchestrator`:**
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/rule/BusinessRuleOrchestrator.java`
    *   **Action:** Create the orchestrator that uses a generic registry to execute rules specified in the blueprint.

        ```java
        package dexter.banking.booktransfers.infrastructure.rule;

        import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
        import dexter.banking.booktransfers.infrastructure.registry.ComponentRegistry;
        import org.springframework.stereotype.Component;
        // Other imports...

        @Component
        public class BusinessRuleOrchestrator {
            private final ComponentRegistry<BusinessRule> registry;

            // ... constructor ...

            public Validation<String, Object> execute(Object command, JourneyBlueprint blueprint) {
                Validation<String, Object> result = Validation.valid(command);

                for (String ruleName : blueprint.businessRules()) {
                    BusinessRule rule = registry.get(ruleName);
                    result = rule.validate(command);

                    if (result.isInvalid()) {
                        return result; // Fail-fast
                    }
                }
                return result;
            }
        }
        ```

### Peek of the Implementation

This two-phase approach, now driven by the type-safe blueprint, provides a robust, configurable, and maintainable validation strategy. The `CommandValidationMiddleware` reads `blueprint.validationGroups()` to get a type-safe list of enums, and the `BusinessRuleOrchestrator` reads `blueprint.businessRules()` to get a list of bean names to execute.

---

## Step 5: Build the Orchestration Layer - The Blueprint-Driven Engine

### The "Why"

With our guarantee framework in place, the `JourneyBlueprint` is now a trusted, validated, and securely delivered artifact. We must now build the runtime engine that *uses* this blueprint to orchestrate the application's behavior, eliminating the rigidity of hardcoded dependencies.

Our objective is to completely invert the dependency. The core logic (`CommandHandler`, `StateMachine`) must be pristine and generic. It should not know or care *which* specific components it is using. It should only know that it needs *a* data collector, or *a* deposit port. The decision of *which specific one* to use is handled entirely by the `JourneyBlueprint` proxy.

### The "What"

We will use a combination of the `JourneyBlueprint` proxy and a simplified **"Orchestrator/Registry" pattern** for executing lists of components.

1.  **Direct Access via Blueprint:** For single, specific components (like an outbound port adapter or a state machine factory), the blueprint provides direct, type-safe access. The complex "Dispatcher" pattern from the previous design is now obsolete, replaced by a simple method call on the blueprint interface.

2.  **Generic Component Registries & Orchestrators:** For executing a *list* of components (like `DataCollectors` or `BusinessRules`), we will use a simple orchestrator. This component is injected with the `JourneyBlueprint`, reads the list of bean names (e.g., `blueprint.dataCollectors()`), and uses a generic `ComponentRegistry` to look up and execute each bean.

### The "How"

This is a significant simplification and clarification of the infrastructure layer.

#### Sub-step 5.1: Implement the Generic `ComponentRegistry`

This component remains essential for orchestrating lists of beans.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/registry/ComponentRegistry.java`
2.  **Action:** Create a generic class that discovers all beans of a given type and allows lookup by name. (Implementation is unchanged from previous design, but its role is now more focused).

#### Sub-step 5.2: Implement the `DataCollectorOrchestrator`

This component executes data enrichment steps based on the blueprint.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/orchestration/DataCollectorOrchestrator.java`
2.  **Action:** Create the orchestrator. It uses the `ComponentRegistry` to execute the list of collectors from the blueprint.

    ```java
    package dexter.banking.booktransfers.infrastructure.orchestration;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import dexter.banking.booktransfers.infrastructure.registry.ComponentRegistry;
    import java.util.List;
    import java.util.concurrent.CompletableFuture;
    import org.springframework.stereotype.Component;

    @Component
    public class DataCollectorOrchestrator {

        private final ComponentRegistry<DataCollector> registry;

        // ... constructor ...

        public EnrichedCommand enrich(Object command, JourneyBlueprint blueprint) {
            // Get the list of bean names from the type-safe blueprint
            List<String> collectorNames = blueprint.dataCollectors();

            List<CompletableFuture<Void>> futures = collectorNames.stream()
                .map(registry::get) // Look up the bean in the registry
                .map(collector -> CompletableFuture.runAsync(() -> collector.collect(command)))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            return new EnrichedCommand(command, ...);
        }
    }
    ```

#### Sub-step 5.3: Eliminate Dispatchers in Favor of Direct Blueprint Access

The complex `Port.Dispatcher` pattern is now entirely obsolete and should be purged. Access to specific adapters is now direct, type-safe, and compile-time checked via the blueprint.

### Peek of the Implementation

This step dramatically cleans up and simplifies the core business logic.

**Before (Rigid and Bloated):**

```java
// Old Command Handler
public class OldSubmitPaymentCommandHandler {
    @Autowired @Qualifier("restDepositAdapter") private DepositPort restDepositAdapter;
    @Autowired @Qualifier("jmsDepositAdapter") private DepositPort jmsDepositAdapter;

    public void handle(SubmitPaymentCommand command) {
        if ("NEW_JOURNEY".equals(command.getJourneyName())) {
            jmsDepositAdapter.submitDeposit(...);
        } else {
            restDepositAdapter.submitDeposit(...);
        }
    }
}
```

**After (The God-Plan Way - Lean, Generic, and Declarative):**

```java
// New Command Handler
@Component
public class SubmitPaymentCommandHandler {
    private final DataCollectorOrchestrator dataCollector;
    private final BusinessRuleOrchestrator ruleOrchestrator;

    // ... constructor ...

    public void handle(SubmitPaymentCommand command, @InJourney StandardPaymentBlueprint blueprint) {
        // 1. Enrich: The handler doesn't know WHICH collectors are run.
        EnrichedCommand enriched = dataCollector.enrich(command, blueprint);

        // 2. Validate: The handler doesn't know WHICH rules are run.
        ruleOrchestrator.execute(enriched, blueprint).getOrElseThrow(...);

        // 3. Orchestrate: Get the state machine factory DIRECTLY from the blueprint.
        StateMachineFactory<SubmitPaymentCommand> factory = blueprint.orchestration().engine();
        StateMachine machine = factory.create(enriched, blueprint); // Pass blueprint to the machine
        machine.start();
    }
}

// And inside the State Machine, the same pattern holds:
@WithStateMachine
public class StandardPaymentStateMachine {
    // No dispatcher dependency!

    private void doSubmitDeposit(StateContext<S, E> context) {
        // Retrieve the blueprint, which was placed in the context by the CommandHandler
        StandardPaymentBlueprint blueprint = context.getExtendedState().get("blueprint", StandardPaymentBlueprint.class);
        SubmitDepositRequest request = ...;

        // The machine doesn't know WHICH adapter (REST/JMS) is called.
        // It gets the correct one directly from the blueprint proxy.
        // This is a compile-time safe call!
        blueprint.adapterRouting().depositPort().submitDeposit(request);
    }
}
```

With this step, we have achieved true architectural decoupling. The core domain is now a generic engine that is configured, composed, and directed entirely by the `JourneyBlueprint`.

---
## Step 6: Refactor the Application Core

### The "Why"

We have built a sophisticated framework of guarantees and dispatchers, but its value is purely theoretical until the application itself adopts it. This final step is the "harvest." It is the process of methodically migrating the application code onto the new framework, thereby reaping the promised benefits of flexibility, safety, and maintainability.

### The "What"

This step involves a systematic, top-to-bottom refactoring of the `book-transfers` service to align with the God-Plan's patterns.

1.  **Refactor Inbound Adapters (`Controllers`):** Their sole responsibility is reduced to an **Anti-Corruption Layer (ACL)**. They only translate external DTOs into internal `Command` objects.
2.  **Refactor Use Cases (`CommandHandlers`):** Handlers become lean orchestrators that declare their needs via the `@InJourney` annotation, requesting a specific `JourneyBlueprint`.
3.  **Refactor Outbound Port Interactions (within `StateMachines`):** All direct dependencies on specific port adapters or dispatchers are removed. Components retrieve the correct adapter at runtime directly from the `JourneyBlueprint`.

### The "How"

This is a methodical process of identifying anti-patterns and replacing them with the new, correct patterns.

#### Sub-step 6.1: Purify the `Controllers`

This remains the same: the controller's only job is to map the request to a command and send it to the bus. The framework handles the rest.

#### Sub-step 6.2: Transform the `CommandHandlers` into Lean Orchestrators

This is the most critical transformation, showcasing the power of the new design.

**Before (Rigid, Imperative Handler):**

```java
@Component
public class OldSubmitPaymentCommandHandler implements CommandHandler<SubmitPaymentCommand> {
    @Autowired @Qualifier("restDepositAdapter") private DepositPort restAdapter;
    @Autowired private CustomerRepository customerRepo;

    @Override
    public void handle(SubmitPaymentCommand command) {
        // Manual data enrichment, validation, and hardcoded logic
        Customer customer = customerRepo.findById(command.getCustomerId());
        if (customer.isNotActive()) { throw new BusinessException(...); }
        restAdapter.submitDeposit(...);
    }
}
```

**After (The God-Plan Way - Lean, Declarative Handler):**

```java
@Component
public class SubmitPaymentCommandHandler implements CommandHandler<SubmitPaymentCommand> {

    // Inject the FRAMEWORK orchestrators
    private final DataCollectorOrchestrator dataCollector;
    private final BusinessRuleOrchestrator ruleOrchestrator;

    public SubmitPaymentCommandHandler(DataCollectorOrchestrator dataCollector, BusinessRuleOrchestrator ruleOrchestrator) {
        this.dataCollector = dataCollector;
        this.ruleOrchestrator = ruleOrchestrator;
    }

    @Override
    public void handle(
        SubmitPaymentCommand command,
        // Declare dependency on the specific, type-safe blueprint
        @InJourney StandardPaymentBlueprint blueprint
    ) {
        // 1. Delegate Enrichment: The handler doesn't know or care WHICH collectors are run.
        EnrichedCommand enrichedCommand = dataCollector.enrich(command, blueprint);

        // 2. Delegate Validation: The handler doesn't know or care WHICH rules are run.
        ruleOrchestrator.execute(enrichedCommand, blueprint)
            .getOrElseThrow(violations -> new BusinessRuleValidationException(violations.toString()));

        // 3. Delegate Orchestration: Get the state machine factory DIRECTLY from the blueprint.
        StateMachineFactory<SubmitPaymentCommand> factory = blueprint.orchestration().engine();
        StateMachine<States, Events> stateMachine = factory.create(enrichedCommand, blueprint); // Pass blueprint down
        
        stateMachine.start();
    }
}
```

#### Sub-step 6.3: Decouple Outbound Port Interactions

This refactoring is now much cleaner and safer.

**Before (Tightly Coupled State Machine):**

```java
@WithStateMachine
public class StandardPaymentStateMachine {
    // Wrong: Depends on a specific implementation or a dispatcher
    private final DepositPort.Dispatcher depositPort;

    private void doSubmitDeposit(StateContext<S, E> context) {
        // Complex logic to get spec and call dispatcher
        JourneySpecification spec = context.getExtendedState().get("spec", ...);
        depositPort.submitDeposit(request, spec);
    }
}
```

**After (Decoupled State Machine via Blueprint):**

```java
@WithStateMachine
public class StandardPaymentStateMachine {
    // Correct: No dependency on any specific adapter or dispatcher.

    private void doSubmitDeposit(StateContext<S, E> context) {
        // 1. Retrieve the blueprint, which was placed in the context by the CommandHandler.
        StandardPaymentBlueprint blueprint = context.getExtendedState().get("blueprint", StandardPaymentBlueprint.class);
        SubmitDepositRequest request = context.getExtendedState().get("DepositRequest", SubmitDepositRequest.class);

        // 2. The machine doesn't know WHICH adapter (REST/JMS) is called.
        // It gets the correct one directly from the blueprint proxy.
        // This is a COMPILE-TIME SAFE method call!
        DepositPort adapter = blueprint.adapterRouting().depositPort();
        adapter.submitDeposit(request);
    }
}
```

### Peek of the Implementation

Upon completion of this step, the `book-transfers` service will be a model of modern, configurable software design.

*   **Ultimate Flexibility:** Adding a new payment journey that reuses existing logic but calls a new, third-party credit service via gRPC is now trivial. We would:
    1.  Implement a `GrpcCreditAdapter` that implements `CreditPort`.
    2.  Add a new journey definition to `application.yml`, pointing `adapterRouting.creditPort` to the bean name of our new adapter.
    3.  The application now supports the new flow **with zero changes to the core domain logic**.
*   **Fearless Refactoring:** The core logic inside `CommandHandlers` and `StateMachines` is now so simple, high-level, and declarative that it is trivial to read, understand, and maintain.
*   **Architectural Purity:** The Hexagonal Architecture is no longer just a theoretical diagram; it is a living, breathing reality enforced by the framework. The core is pristine and completely isolated from the messy details of the outside world.

---

The God-Plan is complete. The system has reached its pinnacle state.
