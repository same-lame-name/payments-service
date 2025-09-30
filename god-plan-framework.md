# The God-Plan Framework: An Architectural Blueprint

**Version:** 6.0
**Status:** Final & Authoritative

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

We will establish a "Type-Safe Blueprint" pattern. This involves a set of artifacts within the `book-transfers/core` module that form the public API of the framework.

1.  **The `JourneyType.java` Enum:** The single, authoritative, and type-safe registry of all known journey types.
2.  **The `JourneyBlueprint.java` Interfaces:** A hierarchy of pure Java interfaces that declaratively define the *shape* of configuration required for a journey.
3.  **The Core Annotations:** A set of annotations (`@InJourney`, `@WithJourneyContext`) that form the developer's interface for interacting with the framework.

### The "How"

#### Sub-step 1.1: Define the `JourneyType` Enum

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/blueprint/JourneyType.java`
2.  **Visibility:** `public`
3.  **Action:** Create the enum. Each constant represents a journey and holds a reference to its corresponding blueprint interface `Class`.

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

#### Sub-step 1.2: Define the `JourneyBlueprint` Interfaces

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
6.  **Action:** Create a concrete blueprint for a standard payment flow.

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

## 3. Step 2 (DEPRECATED): The Public Bean Materializer

<details>
<summary><strong>[CLICK TO EXPAND] DEPRECATED - Version 3.0 Design (Critically Flawed)</strong></summary>

**Reason for Deprecation:** This design is critically flawed as it exposes the `Map<String, JourneySpecification>` as a public Spring bean. This creates a massive, unsecured backdoor that allows any component in the application to inject the entire configuration store, completely undermining the security and monopoly of the `@InJourney` access pattern. It violates the foundational principle of "a developer cannot call an API they cannot see." The correct, secure, and encapsulated design is detailed in **Step 2 (Authoritative): The Secure, Encapsulated Provider & Context Framework**.

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

        public static class JourneysConfigurationProperties {
            private final Map<String, Map<String, Object>> journeys = new java.util.HashMap<>();
            public Map<String, Map<String, Object>> getJourneys() { return journeys; }
        }
    }
    ```

</details>

---

## 4. Step 3 (DEPRECATED): Insecure Context Propagation

<details>
<summary><strong>[CLICK TO EXPAND] DEPRECATED - Version 4.0 Design (Critically Flawed)</strong></summary>

**Reason for Deprecation:** This design, while an improvement, still relied on a public `JourneySpecificationProvider` interface. This still presents an unnecessary public API and a potential backdoor for misuse. The final, authoritative design further restricts visibility by making the provider and its consumers `package-private` and co-locating them, which is a superior and more secure pattern.

</details>

---

## 5. Step 2 (Authoritative): The Secure, Encapsulated Provider & Context Framework

### The "Why"

The framework's security is paramount. Exposing the configuration store, even through a public provider interface, is an unacceptable security risk. It allows any developer to bypass the sanctioned `@InJourney` access pattern, violating our core principle of encapsulation. The framework must enforce its own rules through compiler-guaranteed visibility, not convention.

### The "What"

We will implement a **Secure Encapsulated Provider** pattern. The configuration store will be a `package-private` implementation detail, completely invisible to the application. Only a select few, co-located framework components will be authorized to access it.

1.  **A Secure Package:** A new package, `dexter.banking.booktransfers.infrastructure.provider`, will be created. This package is the security boundary.
2.  **The `BlueprintProvider` (The Store):** A `package-private` class within the secure package that materializes and holds the journey specifications. It is **not** a public bean in the traditional sense.
3.  **Co-located, Authorized Components:** The `ConfigurationEnrichmentMiddleware` and `WithJourneyContextAspect` will be moved into the secure package, granting them exclusive access to the `BlueprintProvider`.
4.  **Compiler Enforcement:** The Java compiler itself will prevent any class outside the `...infrastructure.provider` package from seeing or injecting the `BlueprintProvider`.

### The "How"

#### Sub-step 2.1: Create the Secure Provider Package

1.  **Action:** Create the new package `dexter.banking.booktransfers.infrastructure.provider`.

#### Sub-step 2.2: Implement the `package-private` `BlueprintProvider`

This class is the secure, encapsulated store and materializer.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/provider/BlueprintProvider.java`
2.  **Visibility:** `package-private`
3.  **Action:** Create the provider. It is a Spring `@Component`, but because it is not `public`, it can only be injected within its own package.

    ```java
    package dexter.banking.booktransfers.infrastructure.provider;

    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyBlueprint;
    import dexter.banking.booktransfers.core.domain.shared.blueprint.JourneyType;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import dexter.banking.booktransfers.infrastructure.provider.BlueprintProxyFactory;
    import jakarta.annotation.PostConstruct;
    import org.springframework.boot.context.properties.ConfigurationProperties;
    import org.springframework.context.ApplicationContext;
    import org.springframework.stereotype.Component;
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

        public Map<String, Map<String, Object>> getJourneys() { return journeys; }

        @PostConstruct
        void materializeAndValidateBlueprints() { // RENAMED METHOD
            this.specifications = this.journeys.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                Map.Entry::getKey,
                entry -> {
                    String journeyName = entry.getKey();
                    Map<String, Object> rawConfig = entry.getValue();
                    try {
                        JourneyType journeyType = JourneyType.valueOf((String) rawConfig.get("journeyType"));
                        Class<? extends JourneyBlueprint> blueprintClass = journeyType.getBlueprintClass();
                        JourneyBlueprint blueprintProxy = BlueprintProxyFactory.createProxy(blueprintClass, raw-config, applicationContext);

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
            List<Method> methods = Arrays.asList(proxy.getClass().getInterfaces()[0].getMethods());

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
    ```
#### Sub-step 2.3: Implement the `BlueprintProxyFactory`
Proposed Action 2.3.1: Create the **@BeanReference** Annotation : A new annotation will be created in the core module.File: .../core/src/main/java/dexter/banking/booktransfers/core/domain/shared/blueprint/BeanReference.javaJavapackage dexter.banking.booktransfers.core.domain.shared.blueprint;

1. **File Location** : .../core/src/main/java/dexter/banking/booktransfers/core/domain/shared/blueprint/BeanReference.java
2. **Java package** : dexter.banking.booktransfers.core.domain.shared.blueprint;

    ```java
    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;

    /**
    * Explicitly marks a method on a JourneyBlueprint interface as returning a
    * reference to a Spring Bean. The string value in the corresponding YAML
    * configuration will be interpreted as a bean name.
      */
      @Retention(RetentionPolicy.RUNTIME)
      @Target(ElementType.METHOD)
      public @interface BeanReference {
      }
    ```

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
                    // This exception remains correct.
                    throw new IllegalStateException(String.format(
                        "Configuration key '%s' not found in journey config for blueprint '%s'",
                        key, method.getDeclaringClass().getSimpleName()
                    ));
                }

                if (JourneyBlueprint.class.isAssignableFrom(returnType)) {
                    // This logic for nested blueprints remains correct.
                    return createProxy(
                        (Class<? extends JourneyBlueprint>) returnType,
                        (Map<String, Object>) value,
                        ctx
                    );
                } else if (method.isAnnotationPresent(BeanReference.class)) { // MODIFIED LOGIC
                    // If @BeanReference is present, the value MUST be a string bean name.
                    if (!(value instanceof String)) {
                        throw new IllegalStateException(String.format(
                            "Configuration error: Key '%s' is marked as @BeanReference but value is not a string.", key
                        ));
                    }
                    String beanName = (String) value;
                    if (!ctx.containsBean(beanName)) {
                         throw new IllegalStateException(String.format(
                            "Configuration error: Bean with name '%s' referenced by key '%s' does not exist.",
                            beanName, key
                         ));
                    }
                    return ctx.getBean(beanName, returnType);
                } else {
                    // If no annotation, return the value as a literal.
                    return value;
                }
            }
        }
    }
    ```

---   

#### Sub-step 2.4: Co-locate and Refactor the `ConfigurationEnrichmentMiddleware`

This middleware is moved into the secure package to gain access to the provider.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/provider/ConfigurationEnrichmentMiddleware.java`
2.  **Action:** Modify the middleware to inject the `package-private` `BlueprintProvider`.

    ```java
    package dexter.banking.booktransfers.infrastructure.provider; // MOVED to the secure package

    import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextDeprecated;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextManagerDeprecated;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import dexter.banking.commandbus.Command;
    import dexter.banking.commandbus.Middleware;
    import lombok.RequiredArgsConstructor;
    import org.springframework.core.annotation.Order;
    import org.springframework.stereotype.Component;

    @Order(1)
    @Component
    @RequiredArgsConstructor
    class ConfigurationEnrichmentMiddleware implements Middleware { // Now package-private

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

#### Sub-step 2.5: Co-locate and Refactor the `WithJourneyContextAspect`

This aspect is also moved into the secure package.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/provider/WithJourneyContextAspect.java`
2.  **Action:** Modify the aspect to inject the `package-private` `BlueprintProvider`.

    ```java
    package dexter.banking.booktransfers.infrastructure.provider; // MOVED to the secure package

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

    @Aspect
    @Component
    class WithJourneyContextAspect { // Now package-private

        private final BlueprintProvider blueprintProvider;
        private final SpelExpressionParser expressionParser = new SpelExpressionParser();
        private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

        WithJourneyContextAspect(BlueprintProvider blueprintProvider) {
            this.blueprintProvider = blueprintProvider;
        }

        @Around("@annotation(withJourneyContextDeprecated)")
        public Object establishAdHocContext(ProceedingJoinPoint pjp, WithJourneyContext withJourneyContextDeprecated) throws Throwable {
            Method method = ((MethodSignature) pjp.getSignature()).getMethod();
            MethodBasedEvaluationContext evaluationContext = new MethodBasedEvaluationContext(pjp.getTarget(), method, pjp.getArgs(), parameterNameDiscoverer);
            String journeyName = (String) expressionParser.parseExpression(withJourneyContextDeprecated.journeyIdentifier()).getValue(evaluationContext);

            if (journeyName == null) {
                throw new IllegalStateException("SpEL expression for @WithJourneyContext resolved to null.");
            }

            JourneySpecification spec = blueprintProvider.findByName(journeyName)
                .orElseThrow(() -> new IllegalStateException("No journey specification found for identifier: " + journeyName));

            var context = new JourneyContext(spec);
            try {
                return JourneyContextManager.runWithContext(context, pjp::proceed);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    ```

#### Sub-step 2.6: The Unchanged Secure Access Pattern (`@InJourney`)

The `@InJourney` annotation and its backing `InJourneyParameterAspect` require no changes. They remain blissfully unaware of the provider mechanism. They continue to rely solely on the `public` `JourneyContextManager` to retrieve the context that has been securely established by the now-encapsulated middleware. The security is total.

1.  **`@InJourney` Annotation:** A simple `public` `PARAMETER`-level marker annotation in the `...core.domain.shared.context` package.
2.  **`InJourneyParameterAspect`:** A stateless, `public`, compile-time woven aspect in the `...infrastructure.aspect` package. Its implementation is complete and correct as previously defined.

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
---

## 6. Implementation Status & Roadmap

This section tracks the delivery of the framework components as per the defined MVPs.

### **MVP 1: The Core Blueprint Contracts**
- **Status:** Implemented

#### **Sub-stage 1.1: Foundational Contracts**
- **Status:** Implemented
- **Artifacts Delivered:**
  - `JourneyBlueprint.java`
  - `BeanReference.java`
  - `InJourney.java`
  - `WithJourneyContext.java`

#### **Sub-stage 1.2: Concrete Blueprint Definition**
- **Status:** Implemented
- **Artifacts Delivered:**
  - `StandardPaymentBlueprint.java`
  - `JourneyType.java`

#### **Sub-stage 1.3: Context Data Structures**
- **Status:** Implemented
- **Artifacts Delivered:**
  - `JourneySpecification.java`
  - `JourneyContext.java`
  - `JourneyContextManager.java`

---

### **MVP 2: The Secure Infrastructure Provider**
- **Status:** Implemented

#### **Sub-stage 2.1: The Proxy Factory**
- **Status:** Implemented
- **Objective:** Implement the `BlueprintProxyFactory` responsible for creating dynamic proxies from the configuration.
- **Artifacts Delivered:**
  - `BlueprintProxyFactory.java`

#### **Sub-stage 2.2: The Secure Provider**
- **Status:** Implemented
- **Objective:** Implement the `package-private` `BlueprintProvider` that loads, validates, and caches the journey blueprints.
- **Artifacts Delivered:**
  - `BlueprintProvider.java`

---

### **MVP 3: Context Propagation & Injection**
- **Status:** Implemented

#### **Sub-stage 3.1: Secure Context Establishment**
- **Status:** Implemented
- **Objective:** Implement and co-locate the `ConfigurationEnrichmentMiddleware` and `WithJourneyContextAspect` to securely establish the `JourneyContext`.
- **Artifacts Delivered:**
  - `ConfigurationEnrichmentMiddleware.java`
  - `WithJourneyContextAspect.java`

#### **Sub-stage 3.2: Parameter Injection**
- **Status:** Implemented
- **Objective:** Implement the `InJourneyParameterAspect` to handle the `@InJourney` annotation and inject the correct blueprint.
- **Artifacts Delivered:**
  - `InJourneyParameterAspect.java`
