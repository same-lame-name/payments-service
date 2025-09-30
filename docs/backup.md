# The God-Plan: A Unified Architectural Blueprint

**Version:** 1.0
**Status:** Active & Authoritative

---

## Foreword: The Philosophy of the God-Plan

This document is the single, canonical source of truth for the architectural design and implementation of the payments-service. It supersedes all previous, fragmented `.md` files (`dispatcher-design.md`, `journey-spec-revived.md`, etc.) and consolidates their final, approved, pinnacle designs into one cohesive and unabridged blueprint. All superseded, flawed, or intermediate design concepts have been purged.

Our architecture is founded on one supreme principle: **"Configuration over Code, Guaranteed by the Framework."**

The system must be a lean, generic core whose behavior is dynamically and declaratively dictated by an external configuration artifact—the `JourneySpecification`. This flexibility, however, cannot come at the cost of safety. Therefore, the framework itself must provide an ironclad, multi-layered, and non-bypassable guarantee that this configuration is always valid, complete, and correctly applied for any given context.

This God-Plan is not a collection of suggestions; it is a sequential, top-down implementation strategy. Each step builds upon the successful execution of the previous one, culminating in a system that is scalable, maintainable, resilient, and architecturally pure. This is the final word.

---

## Step 1: Establish the Foundation - Contracts & Configuration

### The "Why"

A configuration-driven system is only as good as the structure of its configuration and the contracts that govern it. Before we can build guarantees, we must first define precisely *what* we are guaranteeing. The current state of the `book-transfers` service has configuration scattered and implicitly understood. We need to formalize this into a robust, type-safe, and explicit structure that can be reasoned about, validated, and consumed by the framework. This step lays the very cornerstone of our entire architecture.

### The "What"

We will establish two primary, interconnected artifacts within the `book-transfers/core` module:

1.  **The `JourneySpecification.java` Record:** A single, immutable, and comprehensive data structure that will serve as the complete definition for any given transaction journey. It is the "DNA" of a request.
2.  **The `JourneyContract.java` Interface:** A pattern for creating pure Java interfaces that declaratively define the *shape* of configuration required by a component. This creates a compile-time link between a piece of code and the structure of the `JourneySpecification` it expects.

### The "How"

This implementation requires a deep understanding of our package structure and visibility principles. Our project adheres to Hexagonal Architecture, with a strict separation of concerns:

*   `book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain`: Contains pure domain objects and contracts. These are the public-facing APIs of our core.
*   `book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure`: Contains adapters and framework-specific implementations (e.g., Aspects, Dispatchers).
*   `book-transfers/app/src/main/java/dexter/banking/booktransfers/app`: The application entry point, responsible for wiring everything together.

Our "no public API" principle means that any framework-internal utility must have `package-private` visibility to prevent application developers from accessing it. However, the core contracts themselves (`JourneySpecification`, `JourneyContract`) are intended to be public within the core's API and thus will be `public`.

#### Sub-step 1.1: Define the `JourneySpecification` Record

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/context/JourneySpecification.java`
2.  **Action:** Create a `public record` that encapsulates all possible points of variation for a journey. We will use nested records to maintain a clean, hierarchical structure.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.context;

    import dexter.banking.booktransfers.core.domain.shared.markers.ValueObject;
    import java.util.List;
    import java.util.Optional;

    /**
     * Represents the complete, immutable configuration for a single transaction journey.
     * This record is the single source of truth that dictates the behavior of the
     * application for a given request. It is deserialized from application.yml.
     */
    public record JourneySpecification(
        String journeyName,
        String journeyContract, // The fully-qualified class name of the JourneyContract
        ValidationConfig validation,
        OrchestrationConfig orchestration,
        AdapterRouting adapterRouting,
        List<String> dataCollectors,
        List<String> businessRules
    ) implements ValueObject {

        /**
         * Defines which syntactic validation rules are active for this journey.
         */
        public record ValidationConfig(
            List<String> groups
        ) implements ValueObject {}

        /**
         * Defines which orchestration engine (e.g., state machine) to use.
         */
        public record OrchestrationConfig(
            String engine // The Spring bean name of the StateMachineFactory
        ) implements ValueObject {}

        /**
         * Provides the Spring bean names for the specific adapter implementations
         * to be used for each outbound port.
         */
        public record AdapterRouting(
            String depositPort,
            String creditPort,
            String limitPort
        ) implements ValueObject {}
    }
    ```

#### Sub-step 1.2: Define the `JourneyContract` Interface and a Concrete Example

1.  **File Location (Base Interface):** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/contract/JourneyContract.java`
2.  **Action:** Create a base marker interface.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.contract;

    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import java.util.List;
    import java.util.function.Function;

    /**
     * Base interface for all Journey Contracts. A contract defines the expected shape
     * and presence of configuration within a JourneySpecification for a component
     * to function correctly.
     */
    public interface JourneyContract {
        /**
         * Returns a list of functions that, when applied to a JourneySpecification,
         * extract the required configuration fields. This is used by the
         * JourneyIntegrityValidator at startup to ensure no required config is null.
         *
         * @return A list of accessor method references.
         */
        List<Function<JourneySpecification, Object>> getRequiredConfigAccessors();
    }
    ```

3.  **File Location (Concrete Contract):** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/contract/StandardPaymentContract.java`
4.  **Action:** Create a concrete contract for a standard payment flow. This demonstrates how a component declares its dependencies on the `JourneySpecification` structure in a compile-time safe way.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.contract;

    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import java.util.List;
    import java.util.function.Function;

    /**
     * A concrete contract for a standard payment journey that requires all three
     * primary outbound ports (deposit, credit, limit).
     */
    public class StandardPaymentContract implements JourneyContract {
        @Override
        public List<Function<JourneySpecification, Object>> getRequiredConfigAccessors() {
            return List.of(
                spec -> spec.adapterRouting().depositPort(),
                spec -> spec.adapterRouting().creditPort(),
                spec -> spec.adapterRouting().limitPort()
            );
        }
    }
    ```

#### Sub-step 1.3: Update `application.yml` to Reflect the New Structure

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/app/src/main/resources/application.yml`
2.  **Action:** Restructure the `journeys` configuration to match the `JourneySpecification` record precisely.

    ```yaml
    app:
      journeys:
        PAYMENT_SUBMIT_V1:
          journeyName: "PAYMENT_SUBMIT_V1"
          journeyContract: "dexter.banking.booktransfers.core.domain.shared.contract.StandardPaymentContract"
          validation:
            groups: ["StandardPayment", "WalletJourney"]
          orchestration:
            engine: "standardPaymentStateMachineFactory"
          adapterRouting:
            depositPort: "DEPOSIT_PORT_REST"
            creditPort: "CREDIT_PORT_JMS"
            limitPort: "LIMIT_PORT_REST"
          dataCollectors: ["customerProfileCollector", "accountBalanceCollector"]
          businessRules: ["sufficientFundsRule", "customerStatusRule"]
        
        # ... other journey definitions
    ```

### The "When"

This step is a **design-time and compile-time** activity. It involves no runtime logic but provides the fundamental data structures and contracts that the entire framework will be built upon in subsequent steps.

### Peek of the Implementation

After this step, the project will not behave differently at runtime. However, its architectural foundation will be immensely stronger. We will have:

*   A single, unambiguous `JourneySpecification.java` file that serves as the "schema" for all journey configurations. Any developer can look at this file and understand exactly what can be configured.
*   A clear `JourneyContract.java` pattern that makes dependencies explicit. A developer working on a `CommandHandler` can now look at its associated contract and know precisely which parts of the `JourneySpecification` it relies on.
*   A clean, structured `application.yml` that directly maps to our core Java record, enabling seamless and error-free deserialization by Spring Boot.

This step replaces ambiguity with clarity and implicit assumptions with explicit, compile-time safe contracts. The stage is now set for building the validation and guarantee layers.

---

## Step 2: Implement Guarantee Layer 1 - Startup-Time Integrity

### The "Why"

An architecture built on configuration is dangerously brittle if that configuration is not verifiable. A simple typographical error in a bean name within `application.yml`, or the omission of a required adapter routing key, must not result in a `NullPointerException` during a transaction in production. Such errors are difficult to trace and erode trust in the system. We must eliminate this entire class of bugs. The guarantee is simple: **the application shall not start if it is configured in a state that guarantees runtime failure.**

### The "What"

We will implement a single, powerful Spring component, the `JourneyIntegrityValidator`. This component will automatically execute once the Spring `ApplicationContext` is fully initialized. It will programmatically load every `JourneySpecification` defined in the environment and perform a comprehensive, two-part validation against the live, fully-wired application context. If any inconsistency is found, it will throw a detailed, human-readable exception that immediately halts application startup.

### The "How"

This component is pure infrastructure and will reside in the `infrastructure` module.

#### Sub-step 2.1: Create the `JourneyIntegrityValidator` Component

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/in/config/JourneyIntegrityValidator.java`
2.  **Action:** Create the class. It will listen for the `ContextRefreshedEvent` to ensure it runs at the correct lifecycle point. It will be injected with the `ApplicationContext` and a map of all `JourneySpecification` beans, which Spring Boot will kindly provide from the YAML configuration.

    ```java
    package dexter.banking.booktransfers.infrastructure.adapter.in.config;

    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import dexter.banking.booktransfers.core.domain.shared.contract.JourneyContract;
    import org.springframework.context.ApplicationContext;
    import org.springframework.context.ApplicationListener;
    import org.springframework.context.event.ContextRefreshedEvent;
    import org.springframework.stereotype.Component;
    import java.util.List;
    import java.util.Map;
    import java.util.Objects;
    import java.util.function.Function;

    @Component
    public class JourneyIntegrityValidator implements ApplicationListener<ContextRefreshedEvent> {

        private final ApplicationContext applicationContext;
        private final Map<String, JourneySpecification> journeySpecifications;

        public JourneyIntegrityValidator(ApplicationContext applicationContext, Map<String, JourneySpecification> journeySpecifications) {
            this.applicationContext = applicationContext;
            this.journeySpecifications = journeySpecifications;
        }

        @Override
        public void onApplicationEvent(ContextRefreshedEvent event) {
            journeySpecifications.values().forEach(this::validateJourney);
        }

        private void validateJourney(JourneySpecification spec) {
            validateContractConformance(spec);
            validateComponentExistence(spec);
        }

        // ... validation methods to be implemented in next sub-steps
    }
    ```

#### Sub-step 2.2: Implement Contract Conformance Validation

1.  **Action:** Add the logic to `JourneyIntegrityValidator` to verify that each journey's configuration fulfills the requirements of its declared `JourneyContract`.

    ```java
    // Inside JourneyIntegrityValidator.java

    private void validateContractConformance(JourneySpecification spec) {
        try {
            Class<?> contractClass = Class.forName(spec.journeyContract());
            JourneyContract contract = (JourneyContract) contractClass.getConstructor().newInstance();
            List<Function<JourneySpecification, Object>> accessors = contract.getRequiredConfigAccessors();

            for (Function<JourneySpecification, Object> accessor : accessors) {
                Object configValue = accessor.apply(spec);
                if (configValue == null || (configValue instanceof String && ((String) configValue).isBlank())) {
                    throw new IllegalStateException(
                        String.format("Journey '%s' failed contract '%s' validation. A required configuration field resolved to null or blank.",
                            spec.journeyName(), spec.journeyContract())
                    );
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                String.format("Journey '%s' specifies an invalid or inaccessible contract class: %s",
                    spec.journeyName(), spec.journeyContract()), e
            );
        }
    }
    ```

#### Sub-step 2.3: Implement Component & Validation Group Existence Validation

1.  **Action:** Add the logic to `JourneyIntegrityValidator` to verify that every bean name and validation group referenced in the YAML corresponds to a real, existing component in the `ApplicationContext` or codebase.

    ```java
    // Inside JourneyIntegrityValidator.java

    private void validateComponentExistence(JourneySpecification spec) {
        // Validate Orchestration Engine
        ensureBeanExists(spec.orchestration().engine(), "orchestration.engine", spec.journeyName());

        // Validate Port Adapters
        ensureBeanExists(spec.adapterRouting().depositPort(), "adapterRouting.depositPort", spec.journeyName());
        ensureBeanExists(spec.adapterRouting().creditPort(), "adapterRouting.creditPort", spec.journeyName());
        ensureBeanExists(spec.adapterRouting().limitPort(), "adapterRouting.limitPort", spec.journeyName());

        // Validate Data Collectors
        spec.dataCollectors().forEach(collector -> ensureBeanExists(collector, "dataCollectors", spec.journeyName()));

        // Validate Business Rules
        spec.businessRules().forEach(rule -> ensureBeanExists(rule, "businessRules", spec.journeyName()));

        // Validate Validation Groups (This requires a predefined ValidationGroups interface)
        // For now, we will assume this check is here. The interface will be created in Step 4.
        // spec.validation().groups().forEach(group -> ensureValidationGroupExists(group, spec.journeyName()));
    }

    private void ensureBeanExists(String beanName, String configPath, String journeyName) {
        if (!applicationContext.containsBean(beanName)) {
            throw new IllegalStateException(
                String.format("Journey '%s' configuration error: Bean with name '%s' referenced in '%s' does not exist.",
                    journeyName, beanName, configPath)
            );
        }
    }
    ```

### The "When"

This component executes automatically, exactly once, during application startup. It is a **boot-time guarantee**. It is the final step of the Spring context initialization before the application is declared "ready."

### Peek of the Implementation

With this step complete, our application gains a powerful immune system. Imagine a developer is adding a new journey, `PAYMENT_SUBMIT_V2`, and they make a typo in the YAML:

```yaml
# application.yml (with typo)
PAYMENT_SUBMIT_V2:
  # ...
  adapterRouting:
    depositPort: "DEPOSIT_PORT_REST_TYPO" # <- Typo!
  # ...
```

**Before this step:** The application would start successfully. The first time a `PAYMENT_SUBMIT_V2` transaction is attempted, the `PortDispatcher` would fail with a `NullPointerException` or a custom "bean not found" error, potentially in a deeply nested, asynchronous call stack, making it difficult to debug.

**After this step:** The application will **refuse to start**. The developer will be greeted with a clear, immediate, and actionable error message on their console:

```
Caused by: java.lang.IllegalStateException: Journey 'PAYMENT_SUBMIT_V2' configuration error: Bean with name 'DEPOSIT_PORT_REST_TYPO' referenced in 'adapterRouting.depositPort' does not exist.
```

This moves the feedback loop from a potential production incident to an immediate boot-time failure, enforcing architectural integrity and saving countless hours of debugging. The system is now fundamentally safer.

---

## Step 3: Implement Guarantee Layer 2 - Secure Context & Runtime Contract Enforcement

### The "Why"

Our architecture is critically dependent on the `JourneySpecification` being available to any component that needs it. This presents a dangerous, two-headed problem:

1.  **The Problem of Assumed Context:** How do we provide this context? Relying on developers to manually wrap every possible entry point (controllers, listeners, schedulers) in `ScopedValue.where(...).call(...)` is a policy of hope, not a strategy. This "hopeful coverage" is fragile and destined to fail, leading to `NoSuchElementException`s in production when an untested code path is executed without the proper scope being established.

2.  **The Problem of the Docile Bodyguard:** Even if we provide a context, how do we ensure it's the *correct* one for the code being executed? A component might require a `JourneySpecification` that has adapter routing configured, but it could be invoked within a journey that lacks it. Furthermore, previous designs considered a static accessor (`JourneyContext.get()`) guarded by an annotation. This is a security flaw; a developer could simply call the static accessor from any method, completely ignoring the annotation and its validation guards.

We need a system that is **non-bypassable** and **self-validating**. The framework must not simply *provide* the context; it must *inject* it, and this injection process itself must be the unyielding gatekeeper that validates the context against the component's declared contract. The safe path must be the *only* path.

### The "What"

We will implement the **"Argument Injection Model,"** a definitive and architecturally pure design that solves both problems simultaneously. This model is founded on a simple, unbreakable principle: **You cannot call an API that you cannot see.**

We will eliminate all static accessors for the journey context. The `JourneySpecification` will no longer be "retrieved" by developer code. Instead, it will be **injected** directly by the framework as a method parameter. This makes the dependency explicit, visible, and guaranteed by the method's own signature.

This system consists of three core components working in concert:

1.  **The `@InJourney` Annotation:** A `PARAMETER`-level annotation. It is the developer's declarative request for the framework to inject the `JourneySpecification`. Crucially, it contains a `requires` attribute to specify the `JourneyContract` that the context must fulfill.
2.  **The `JourneyContext` Utility:** A **sealed, `package-private` utility** with **zero public API**. It is a hidden mechanism, a "shared secret" between the framework's entry-point wrapper and the injection aspect. Its sole purpose is to bind the `JourneySpecification` to a `java.lang.ScopedValue` and provide a hidden accessor for the aspect to retrieve it.
3.  **The `InJourneyParameterAspect`:** A powerful `@Around` aspect that intercepts the execution of any method that has a parameter annotated with `@InJourney`. This is the engine of our guarantee, responsible for retrieving the context, validating it against the required contract, and injecting it into the method call.

### The "How"

The implementation is a precise orchestration between the core contracts, infrastructure aspects, and web filters.

#### Sub-step 3.1: Create the `@InJourney` Annotation

This annotation is a core contract, defining the interaction between application code and the framework.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/context/InJourney.java`
2.  **Action:** Create the annotation. It targets only parameters and includes the `requires` attribute for contract validation.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.context;

    import dexter.banking.booktransfers.core.domain.shared.contract.JourneyContract;
    import java.lang.annotation.ElementType;
    import java.lang.annotation.Retention;
    import java.lang.annotation.RetentionPolicy;
    import java.lang.annotation.Target;

    /**
     * Marks a method parameter to be injected with the current JourneySpecification.
     * This is the only sanctioned way for application code to access the journey context.
     * The framework guarantees that the injected parameter will be non-null and,
     * if a contract is specified, that the JourneySpecification fulfills that contract.
     */
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InJourney {
        /**
         * Specifies a required JourneyContract interface that the JourneySpecification
         * must conform to. If the contract is not met, the framework will throw a
         * ContractViolationException before the method is executed.
         *
         * @return The required JourneyContract class.
         */
        Class<? extends JourneyContract> requires() default JourneyContract.class; // Default is the base, empty contract
    }
    ```

#### Sub-step 3.2: Create the Sealed `JourneyContext` Utility

This is the hidden heart of the context propagation mechanism. Its `package-private` visibility is non-negotiable.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/context/JourneyContext.java`
2.  **Action:** Create the `final` class with a `private ScopedValue` and `package-private` methods.

    ```java
    package dexter.banking.booktransfers.core.domain.shared.context;

    import java.util.concurrent.Callable;

    /**
     * A sealed, framework-internal utility for managing the JourneySpecification's lifecycle
     * via a ScopedValue. This class has NO PUBLIC API and is not intended for use by
     * application developers. It is a shared secret between the entry-point filter/aspect
     * and the InJourneyParameterAspect.
     */
    public final class JourneyContext {

        private static final ScopedValue<JourneySpecification> SCOPED_SPEC = ScopedValue.newInstance();

        private JourneyContext() {
            // Prevent instantiation
        }

        /**
         * [PACKAGE-PRIVATE] Binds the given JourneySpecification to the scope for the
         * duration of the provided operation.
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

To bind the `ScopedValue`, we need to wrap the entire request processing chain. A standard Servlet `Filter` is the perfect tool for this.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/in/web/JourneyScopeFilter.java`
2.  **Action:** Create a `Filter` that extracts a journey identifier from the request, finds the corresponding `JourneySpecification`, and uses our sealed `JourneyContext` to bind it for the request's duration.

    ```java
    package dexter.banking.booktransfers.infrastructure.adapter.in.web;

    import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextDeprecated;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import jakarta.servlet.*;
    import jakarta.servlet.http.HttpServletRequest;
    import org.springframework.stereotype.Component;
    import java.io.IOException;
    import java.util.Map;
    import java.util.Objects;

    @Component
    public class JourneyScopeFilter implements Filter {

        private final Map<String, JourneySpecification> journeySpecifications;
        private static final String JOURNEY_NAME_HEADER = "X-Journey-Name";

        public JourneyScopeFilter(Map<String, JourneySpecification> journeySpecifications) {
            this.journeySpecifications = journeySpecifications;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

            HttpServletRequest httpRequest = (HttpServletRequest) request;
            String journeyName = httpRequest.getHeader(JOURNEY_NAME_HEADER);

            if (journeyName == null || journeyName.isBlank()) {
                // For requests that don't specify a journey, proceed without a context
                chain.doFilter(request, response);
                return;
            }

            JourneySpecification spec = journeySpecifications.get(journeyName);
            if (spec == null) {
                // Or handle as an error response
                throw new ServletException("Invalid journey name specified in " + JOURNEY_NAME_HEADER + " header: " + journeyName);
            }

            try {
                JourneyContext.runWith(spec, () -> {
                    chain.doFilter(request, response);
                    return null; // Callable needs a return value
                });
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
    }
    ```

#### Sub-step 3.4: Implement the `InJourneyParameterAspect`

This is the engine that enforces our guarantee at the point of use.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/aspect/InJourneyParameterAspect.java`
2.  **Action:** Create the `@Around` aspect. It finds the annotated parameter, retrieves and validates the context, and injects it.

    ```java
    package dexter.banking.booktransfers.infrastructure.aspect;

    import dexter.banking.booktransfers.core.domain.shared.context.InJourney;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextDeprecated;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import dexter.banking.booktransfers.core.domain.shared.contract.JourneyContract;
    import org.aspectj.lang.ProceedingJoinPoint;
    import org.aspectj.lang.annotation.Around;
    import org.aspectj.lang.annotation.Aspect;
    import org.aspectj.lang.reflect.MethodSignature;
    import org.springframework.stereotype.Component;

    import java.lang.reflect.Method;
    import java.lang.reflect.Parameter;

    @Aspect
    @Component
    public class InJourneyParameterAspect {

        @Around("execution(* *(.., @dexter.banking.booktransfers.core.domain.shared.context.InJourney (*), ..))")
        public Object injectJourneyContext(ProceedingJoinPoint pjp) throws Throwable {
            // 1. Retrieve the current JourneySpecification
            JourneySpecification spec = JourneyContext.getSpec(); // Throws if not bound, which is a critical failure

            // 2. Find the annotated parameter and the annotation instance
            MethodSignature signature = (MethodSignature) pjp.getSignature();
            Method method = signature.getMethod();
            Parameter[] parameters = method.getParameters();
            Object[] args = pjp.getArgs();

            int injectionIndex = -1;
            InJourney annotation = null;

            for (int i = 0; i < parameters.length; i++) {
                if (parameters[i].isAnnotationPresent(InJourney.class)) {
                    injectionIndex = i;
                    annotation = parameters[i].getAnnotation(InJourney.class);
                    break;
                }
            }

            // 3. Validate the contract
            Class<? extends JourneyContract> contractClass = annotation.requires();
            if (contractClass != JourneyContract.class) { // Check if a specific contract is required
                try {
                    JourneyContract requiredContract = contractClass.getConstructor().newInstance();
                    // Use the logic from our startup validator to check conformance
                    requiredContract.getRequiredConfigAccessors().forEach(accessor -> {
                        Object configValue = accessor.apply(spec);
                        if (configValue == null || (configValue instanceof String && ((String) configValue).isBlank())) {
                            throw new ContractViolationException(
                                String.format("Runtime contract violation in method '%s'. Journey '%s' does not fulfill contract '%s'. A required field is missing.",
                                    method.getName(), spec.journeyName(), contractClass.getSimpleName())
                            );
                        }
                    });
                } catch (Exception e) {
                    throw new IllegalStateException("Could not instantiate JourneyContract: " + contractClass.getName(), e);
                }
            }

            // 4. Inject the spec into the arguments array
            args[injectionIndex] = spec;

            // 5. Proceed with the original method call with modified arguments
            return pjp.proceed(args);
        }

        // A simple runtime exception for contract failures
        public static class ContractViolationException extends RuntimeException {
            public ContractViolationException(String message) {
                super(message);
            }
        }
    }
    ```

### The "When"

This is a **runtime guarantee**. The `JourneyScopeFilter` runs at the very beginning of every applicable HTTP request. The `InJourneyParameterAspect` runs at the last possible moment, just before the execution of any method that declares a dependency on the journey context.

### Peek of the Implementation

This step fundamentally transforms the developer experience and the safety of the system.

**Before (Flawed & Unsafe):**

```java
// A developer might have done this, which is verbose, unsafe, and bypassable.
public class SomeHandler {
    public void handle(SomeCommand command) {
        // Unsafe, non-validated, assumes context is present.
        JourneySpecification spec = SomeStaticContextHolder.getSpec();
        // ... logic that might fail if spec is wrong ...
    }
}
```

**After (The God-Plan Way - Safe & Declarative):**

```java
// The dependency is explicit, validated, and guaranteed by the framework.
public class SomeHandler {
    public void handle(SomeCommand command,
                       @InJourney(requires = StandardPaymentContract.class) JourneySpecification spec) {
        // The 'spec' variable is GUARANTEED by the framework to be:
        // 1. Non-null.
        // 2. The correct JourneySpecification for the current request.
        // 3. Validated against the StandardPaymentContract BEFORE this line is ever reached.

        String depositAdapterBeanName = spec.adapterRouting().depositPort(); // This is safe to call.
        // ...
    }
}
```

With this step, we have achieved an unbreakable chain of custody for our context. There is no static API to abuse. The dependency is a compile-time fact. The validation is a runtime guarantee. The safe lane is now the only lane.

---

## Step 4: Implement Guarantee Layer 3 - The Two-Phase Validation Framework

### The "Why"

A request entering our system must pass through two distinct validation gates before it can be trusted. Confusing these two gates leads to bloated, unmaintainable components and violates the Single Responsibility Principle.

1.  **Syntactic & API Contract Validation:** This phase answers the question: **"Did the caller use our API correctly according to the journey's specific rules?"** It validates the *shape*, *format*, and *presence* of data in the incoming `Command` DTO. For example, is a field that's mandatory for "Journey A" present? Is a string field a valid UUID? This validation must happen at the earliest possible moment to **fail fast**, rejecting malformed requests before any expensive operations (like database lookups or external API calls) are attempted.

2.  **Semantic & Business Rule Validation:** This phase occurs later and answers the much deeper question: **"Does this validly-formed request represent a valid business operation?"** For example, does the customer have sufficient funds? Is their account status active? This validation requires a complete, enriched data context (e.g., the customer's current balance, fetched from another service) and represents the core business logic of our domain.

We require a dedicated, declarative framework for each phase, ensuring a clean separation of concerns and aligning with our "configuration over code" philosophy.

### The "What"

We will implement a two-phase validation strategy, with each phase powered by its own specialized, configuration-driven framework.

1.  **Phase 1: The Context-Aware Syntactic Validation Framework.** We will build a `CommandValidationMiddleware` that sits between the web adapter and the command handler. This middleware will use the standard **Java Bean Validation (JSR 303)** framework, but its power will come from using **Validation Groups**. The specific validation groups to be activated will be read directly from the `JourneySpecification` (`validation.groups` list), making the API contract validation dynamically configurable per journey.

2.  **Phase 2: The Declarative Business Rule Engine.** We will build a home-grown, annotation-driven rule engine for semantic validation. This engine will be invoked from within the `CommandHandler` *after* all necessary data has been collected. It will be driven by a list of rule names in the `JourneySpecification` (`businessRules` list) and will execute rules in a declarative **`Given-When-Then`** style.

### The "How"

The implementation is cleanly divided into the two phases, with components spanning the `core` and `infrastructure` modules.

#### Sub-step 4.1: Implement the Syntactic Validation Framework

1.  **Create the `ValidationGroups` Namespace:**
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/validation/ValidationGroups.java`
    *   **Action:** Create a public interface to act as a type-safe container for all our validation group marker interfaces.

        ```java
        package dexter.banking.booktransfers.core.domain.shared.validation;

        /**
         * A namespace interface for all JSR 303 validation groups.
         * This provides a single, discoverable location for all rule sets.
         */
        public interface ValidationGroups {
            /**
             * Validation rules applicable to a standard payment submission.
             */
            interface StandardPayment {}

            /**
             * Validation rules applicable to a wallet top-up operation.
             */
            interface WalletJourney {}
        }
        ```

2.  **Annotate a `Command` DTO:**
    *   **File Location:** (Example) `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/port/in/SubmitPaymentCommand.java`
    *   **Action:** Apply standard JSR 303 annotations to the command's fields, assigning each one to one or more validation groups. These annotations are **dormant** by default.

        ```java
        // Inside SubmitPaymentCommand.java
        import jakarta.validation.constraints.NotNull;
        import jakarta.validation.constraints.DecimalMin;
        import java.math.BigDecimal;

        public record SubmitPaymentCommand(
            // This field is required for ALL journeys that use this command.
            @NotNull(groups = {ValidationGroups.StandardPayment.class, ValidationGroups.WalletJourney.class})
            String journeyName,

            // This field is only required for a standard payment.
            @NotNull(groups = ValidationGroups.StandardPayment.class)
            String targetAccountId,

            // This field is only required for a wallet top-up.
            @NotNull(groups = ValidationGroups.WalletJourney.class)
            String walletId,

            @NotNull(groups = {ValidationGroups.StandardPayment.class, ValidationGroups.WalletJourney.class})
            @DecimalMin(value = "0.01", groups = {ValidationGroups.StandardPayment.class, ValidationGroups.WalletJourney.class})
            BigDecimal amount
        ) {
            // ...
        }
        ```

3.  **Implement the `CommandValidationMiddleware`:**
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/middleware/CommandValidationMiddleware.java`
    *   **Action:** Create a middleware component (e.g., an aspect around the `CommandBus.send()` method) that intercepts the command, reads the active validation groups from the `JourneySpecification`, and invokes the validator.

        ```java
        package dexter.banking.booktransfers.infrastructure.middleware;

        import dexter.banking.booktransfers.core.domain.shared.context.InJourney;
        import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
        import jakarta.validation.ConstraintViolation;
        import jakarta.validation.ConstraintViolationException;
        import jakarta.validation.Validator;
        import org.aspectj.lang.JoinPoint;
        import org.aspectj.lang.annotation.Aspect;
        import org.aspectj.lang.annotation.Before;
        import org.springframework.stereotype.Component;
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
            public void validateCommand(JoinPoint jp, Object command, @InJourney JourneySpecification spec) {
                // Resolve group names from YAML to Class objects
                Set<Class<?>> activeGroups = spec.validation().groups().stream()
                    .map(this::resolveGroup)
                    .collect(Collectors.toSet());

                if (activeGroups.isEmpty()) {
                    return; // No validation configured for this journey
                }

                // Execute validation using only the active groups
                Set<ConstraintViolation<Object>> violations = validator.validate(command, activeGroups.toArray(new Class[0]));

                if (!violations.isEmpty()) {
                    throw new ConstraintViolationException(violations);
                }
            }

            private Class<?> resolveGroup(String groupName) {
                try {
                    // This is a simplified resolution. A real implementation would be more robust,
                    // likely scanning the ValidationGroups interface.
                    return Class.forName("dexter.banking.booktransfers.core.domain.shared.validation.ValidationGroups$" + groupName);
                } catch (ClassNotFoundException e) {
                    // This should be caught at startup by the JourneyIntegrityValidator, but we defend here too.
                    throw new IllegalStateException("Configuration error: Validation group not found: " + groupName, e);
                }
            }
        }
        ```

#### Sub-step 4.2: Implement the Semantic Business Rule Engine

1.  **Create the Rule Engine Annotations:**
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/core/src/main/java/dexter/banking/booktransfers/core/domain/shared/rule/`
    *   **Action:** Create the `@BusinessRule`, `@When`, and `@Then` annotations in the `core` module.

        ```java
        // @BusinessRule.java
        @Target(ElementType.TYPE)
        @Retention(RetentionPolicy.RUNTIME)
        @Component // Makes the rule a discoverable Spring bean
        public @interface BusinessRule {
            String value(); // The unique bean name of the rule
        }

        // @When.java
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface When {}

        // @Then.java
        @Target(ElementType.METHOD)
        @Retention(RetentionPolicy.RUNTIME)
        public @interface Then {}
        ```

2.  **Implement a Sample Business Rule:**
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/rule/SufficientFundsRule.java`
    *   **Action:** Create a concrete rule that checks if a customer has sufficient funds. This requires an enriched command object.

        ```java
        package dexter.banking.booktransfers.infrastructure.rule;

        import dexter.banking.booktransfers.core.domain.shared.rule.*;
        import io.vavr.control.Validation; // Using Vavr for functional validation results
        import java.math.BigDecimal;

        @BusinessRule("sufficientFundsRule")
        public class SufficientFundsRule {

            // The "Guard" - checks if the rule is applicable
            @When
            public boolean isApplicable(EnrichedSubmitPaymentCommand command) {
                // Only run this rule if we have successfully fetched the account balance
                return command.getAccountBalance() != null;
            }

            // The "Action" - contains the core validation logic
            @Then
            public Validation<String, EnrichedSubmitPaymentCommand> validate(EnrichedSubmitPaymentCommand command) {
                if (command.getAccountBalance().compareTo(command.getAmount()) >= 0) {
                    return Validation.valid(command); // Success
                } else {
                    return Validation.invalid("Insufficient funds. Balance: " + command.getAccountBalance()); // Failure
                }
            }
        }
        ```

3.  **Implement the `BusinessRuleRegistry` and `BusinessRuleOrchestrator`:**
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/rule/`
    *   **Action:** Create the engine components. The `Registry` will discover and cache rules on startup. The `Orchestrator` will execute them at runtime. (Implementation is complex and shown conceptually).

        ```java
        // BusinessRuleOrchestrator.java (Conceptual)
        @Component
        public class BusinessRuleOrchestrator {
            private final BusinessRuleRegistry registry;

            // ... constructor ...

            public Validation<String, Object> execute(Object command, @InJourney JourneySpecification spec) {
                Validation<String, Object> result = Validation.valid(command);

                for (String ruleName : spec.businessRules()) {
                    // 1. Get rule from registry
                    BusinessRuleExecutor rule = registry.getExecutor(ruleName);

                    // 2. Check the @When guard
                    if (rule.isApplicable(command)) {
                        // 3. Execute the @Then logic
                        result = rule.execute(command);

                        // 4. Fail-fast: stop on the first invalid result
                        if (result.isInvalid()) {
                            return result;
                        }
                    }
                }
                return result;
            }
        }
        ```

### The "When"

This is a **runtime guarantee** with two distinct timings:

*   **Syntactic Validation:** Executes in middleware, **immediately after** the `Command` is created from the incoming request, and **before** it is passed to the `CommandBus`.
*   **Semantic Validation:** Executes within the `CommandHandler`, **after** all data collection and enrichment is complete, and **before** the core transaction logic (e.g., invoking a state machine or outbound port) begins.

### Peek of the Implementation

This step creates a clear and powerful validation pipeline.

1.  A request comes in for "Journey A" with a missing `targetAccountId`.
2.  The `Controller` creates the `SubmitPaymentCommand`.
3.  The `CommandValidationMiddleware` intercepts the command. It reads the `JourneySpecification` for "Journey A," sees that the `StandardPayment` group is active, and invokes the validator.
4.  The `@NotNull(groups = StandardPayment.class)` annotation on `targetAccountId` is triggered.
5.  A `ConstraintViolationException` is thrown. An exception handler maps this to a **400 Bad Request** response with a clear error message. The `CommandHandler` is **never even called**.

Now, imagine a valid request comes in.

1.  The syntactic validation passes.
2.  The `CommandHandler` is invoked. It first calls a `DataCollectorOrchestrator` to fetch the customer's balance, enriching the command.
3.  The handler then calls `businessRuleOrchestrator.execute(...)`.
4.  The orchestrator reads `businessRules: ["sufficientFundsRule"]` from the spec.
5.  It executes `SufficientFundsRule`. The `@When` guard passes. The `@Then` logic finds the balance is too low and returns `Validation.invalid("Insufficient funds...")`.
6.  The orchestrator immediately returns this invalid result.
7.  The `CommandHandler` catches this, throws a `BusinessRuleValidationException`, which is mapped to a **422 Unprocessable Entity** response. The transaction is halted before any money is moved.

This two-phase approach provides a robust, configurable, and maintainable validation strategy that protects the integrity of our core domain.

---

## Step 5: Build the Orchestration Layer - The Dispatcher Ecosystem

### The "Why"

With our guarantee framework in place, the `JourneySpecification` is now a trusted, validated, and securely delivered artifact. However, it is currently just data. To achieve our primary goal, we must build the runtime engine that actually *uses* this data to orchestrate the application's behavior.

The core problem is **rigidity**. In a traditional architecture, a `CommandHandler` that needs to call a deposit service would be directly injected with a specific implementation (e.g., `RestDepositAdapter`). If we later need to support a `JmsDepositAdapter` for a different journey, we would be forced to modify the `CommandHandler` with ugly `if/else` logic or `@Qualifier` annotations. This violates the Open/Closed Principle and leads to a brittle, unscalable system where every new requirement forces changes to the core code.

Our objective is to completely invert this dependency. The core logic (`CommandHandler`, `StateMachine`) must be pristine and generic. It should not know or care *which* specific components it is using. It should only know that it needs *a* data collector, or *a* deposit port. The decision of *which specific one* to use must be delegated to a lower-level infrastructure component that is driven entirely by the `JourneySpecification`.

### The "What"

We will build a consistent and reusable **"Dispatcher/Registry" pattern** for every point of variation in our architecture. This ecosystem is the heart of our runtime engine, responsible for translating the declarative configuration from the `JourneySpecification` into concrete application behavior.

This ecosystem will consist of two primary types of components, all residing in the `infrastructure` module:

1.  **Generic Component Registries:** These are Spring beans that execute at startup. For each type of pluggable component (`DataCollector`, `BusinessRule`, `PortAdapter`, `StateMachineFactory`), a registry will discover all available implementations from the `ApplicationContext` and store them in a `Map<String, ComponentInterface>`, keyed by their unique Spring bean name.

2.  **Generic Dispatchers & Orchestrators:** These are the runtime engines that are injected into our core domain logic. They receive the `JourneySpecification` (via the `@InJourney` annotation from Step 3) and use it to query the appropriate registry. For example, a `PortDispatcher` will read the `adapterRouting` key, while a `DataCollectorOrchestrator` will read the `dataCollectors` list.

We will implement this pattern for four key areas:
*   **Data Collection:** An orchestrator to run enrichment components in parallel.
*   **Outbound Ports:** A dispatcher for each outbound port to select the correct adapter (e.g., REST vs. JMS).
*   **State Machine Selection:** A dispatcher to select the correct `StateMachineFactory` for the journey.
*   **Business Rule Execution:** The `BusinessRuleOrchestrator` from Step 4 is already an example of this pattern.

### The "How"

This is a significant infrastructure build-out. We will create a set of generic, reusable framework components.

#### Sub-step 5.1: Implement the Generic `ComponentRegistry`

To avoid code duplication, we will create a single, generic registry class that can be parameterized for any component type.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/registry/ComponentRegistry.java`
2.  **Action:** Create a generic class that takes a list of components, discovers their bean names, and builds a lookup map.

    ```java
    package dexter.banking.booktransfers.infrastructure.registry;

    import org.springframework.beans.factory.NoSuchBeanDefinitionException;
    import org.springframework.context.ApplicationContext;
    import java.util.List;
    import java.util.Map;
    import java.util.function.Function;
    import java.util.stream.Collectors;

    /**
     * A generic, reusable component that discovers and registers all beans of a given
     * type, allowing for lookup by their Spring bean name.
     * @param <T> The type of the component interface to register.
     */
    public class ComponentRegistry<T> {

        private final Map<String, T> registry;

        public ComponentRegistry(Class<T> type, ApplicationContext context) {
            this.registry = context.getBeansOfType(type).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        /**
         * Retrieves a component instance by its bean name.
         * @throws IllegalStateException if the bean name is not found.
         */
        public T get(String beanName) {
            T component = registry.get(beanName);
            if (component == null) {
                // This should have been caught by the JourneyIntegrityValidator at startup.
                throw new IllegalStateException(
                    String.format("Component implementation with bean name '%s' not found in registry.", beanName)
                );
            }
            return component;
        }
    }
    ```

3.  **Action:** In a configuration class, define the specific registry beans we need.
    *   **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/config/RegistryConfiguration.java`

    ```java
    package dexter.banking.booktransfers.infrastructure.config;
    // imports for DataCollector, BusinessRule, StateMachineFactory, etc.
    import org.springframework.context.ApplicationContext;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;

    @Configuration
    public class RegistryConfiguration {

        @Bean
        public ComponentRegistry<DataCollector> dataCollectorRegistry(ApplicationContext context) {
            return new ComponentRegistry<>(DataCollector.class, context);
        }

        @Bean
        public ComponentRegistry<BusinessRule> businessRuleRegistry(ApplicationContext context) {
            // Assuming BusinessRule is the interface for our rules from Step 4
            return new ComponentRegistry<>(BusinessRule.class, context);
        }

        @Bean
        public ComponentRegistry<StateMachineFactory> stateMachineFactoryRegistry(ApplicationContext context) {
            return new ComponentRegistry<>(StateMachineFactory.class, context);
        }
    }
    ```

#### Sub-step 5.2: Implement the `DataCollectorOrchestrator`

This component will execute data enrichment steps in parallel for maximum performance.

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/orchestration/DataCollectorOrchestrator.java`
2.  **Action:** Create the orchestrator. It uses the `dataCollectorRegistry` and `CompletableFuture` to run collectors concurrently.

    ```java
    package dexter.banking.booktransfers.infrastructure.orchestration;

    // imports...
    import dexter.banking.booktransfers.infrastructure.registry.ComponentRegistry;
    import java.util.List;
    import java.util.concurrent.CompletableFuture;

    @Component
    public class DataCollectorOrchestrator {

        private final ComponentRegistry<DataCollector> registry;

        // ... constructor ...

        public EnrichedCommand enrich(Object command, @InJourney JourneySpecification spec) {
            List<CompletableFuture<Void>> futures = spec.dataCollectors().stream()
                .map(registry::get)
                .map(collector -> CompletableFuture.runAsync(() -> collector.collect(command)))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // The assumption here is that collectors mutate a shared, thread-safe
            // context object or that the command itself is designed for concurrent enrichment.
            // For this example, we'll assume it returns a new enriched object.
            return new EnrichedCommand(command, ...);
        }
    }
    ```

#### Sub-step 5.3: Implement the Pluggable Port Dispatcher Pattern

This is the pinnacle of our Hexagonal Architecture implementation.

1.  **Define Core Contracts:**
    *   **Action:** For each outbound port (e.g., `DepositPort`), define a `Dispatcher` marker interface. The core domain will *only* be injected with this type.
    *   **File Location:** `.../core/port/out/DepositPort.java`

        ```java
        public interface DepositPort {
            // Methods now explicitly require the JourneySpecification
            DebitLegResult submitDeposit(SubmitDepositRequest request, JourneySpecification spec);
            // ...

            /**
             * A marker interface for the dispatcher implementation. The core domain
             * should ALWAYS be injected with this type.
             */
            interface Dispatcher extends DepositPort {}
        }
        ```

2.  **Implement the Concrete `DepositPortDispatcher`:**
    *   **File Location:** `.../infrastructure/adapter/out/deposit/DepositPortDispatcher.java`
    *   **Action:** This dispatcher implements the port's `Dispatcher` interface. It is injected with *all* available implementations of `DepositPort` and builds its own internal registry.

    ```java
    package dexter.banking.booktransfers.infrastructure.adapter.out.deposit;

    // imports...
    import org.springframework.context.ApplicationContext;
    import org.springframework.stereotype.Component;
    import java.util.List;
    import java.util.Map;
    import java.util.stream.Collectors;

    @Component("depositPortDispatcher") // Give it a well-known name
    public class DepositPortDispatcher implements DepositPort.Dispatcher {

        private final Map<String, DepositPort> adapterMap;

        public DepositPortDispatcher(List<DepositPort> adapters, ApplicationContext context) {
            // Build the map, filtering out itself to prevent infinite recursion.
            this.adapterMap = adapters.stream()
                .filter(adapter -> !(adapter instanceof DepositPort.Dispatcher))
                .collect(Collectors.toMap(
                    adapter -> context.getBeanNamesForType(adapter.getClass())[0],
                    Function.identity()
                ));
        }

        @Override
        public DebitLegResult submitDeposit(SubmitDepositRequest request, JourneySpecification spec) {
            String adapterName = spec.adapterRouting().depositPort();
            DepositPort adapter = adapterMap.get(adapterName);
            if (adapter == null) {
                // Should be caught at startup, but we defend here.
                throw new IllegalStateException("DepositPort bean not found for name: '" + adapterName + "'");
            }
            // Delegate to the selected adapter. Note we pass the spec down.
            return adapter.submitDeposit(request, spec);
        }
        // ... other methods
    }
    ```

### The "When"

This is a **runtime** mechanism. The registries are built once at startup, but the dispatching and orchestration logic executes dynamically for every single request, driven by the `JourneySpecification` provided by our secure context framework from Step 3.

### Peek of the Implementation

This step dramatically cleans up and simplifies the core business logic, making it purely declarative.

**Before (Rigid and Bloated):**

```java
@Component
public class OldSubmitPaymentCommandHandler {
    @Autowired @Qualifier("restDepositAdapter")
    private DepositPort restDepositAdapter;

    @Autowired @Qualifier("jmsDepositAdapter")
    private DepositPort jmsDepositAdapter;

    public void handle(SubmitPaymentCommand command) {
        // ...
        if ("NEW_JOURNEY".equals(command.getJourneyName())) {
            jmsDepositAdapter.submitDeposit(...);
        } else {
            restDepositAdapter.submitDeposit(...);
        }
        // ...
    }
}
```

**After (The God-Plan Way - Lean, Generic, and Declarative):**

```java
@Component
public class SubmitPaymentCommandHandler {

    private final DataCollectorOrchestrator dataCollector;
    private final BusinessRuleOrchestrator ruleOrchestrator;
    private final StateMachineFactoryDispatcher stateMachineDispatcher;

    // ... constructor ...

    public void handle(SubmitPaymentCommand command,
                       @InJourney(requires = StandardPaymentContract.class) JourneySpecification spec) {

        // 1. Enrich: The handler doesn't know WHICH collectors are run.
        EnrichedCommand enriched = dataCollector.enrich(command, spec);

        // 2. Validate: The handler doesn't know WHICH rules are run.
        ruleOrchestrator.execute(enriched, spec)
            .getOrElseThrow(BusinessRuleValidationException::new);

        // 3. Orchestrate: The handler doesn't know WHICH state machine is used.
        StateMachineFactory factory = stateMachineDispatcher.getFactory(spec);
        StateMachine machine = factory.create(enriched, spec); // Pass spec to the machine
        machine.start();
    }
}

// And inside the State Machine, the same pattern holds:
@WithStateMachine
public class StandardPaymentStateMachine {
    private final DepositPort.Dispatcher depositPort; // Injected with the DISPATCHER

    // ...
    private void doSubmitDeposit(StateContext<S, E> context) {
        JourneySpecification spec = context.getExtendedState().get("spec", JourneySpecification.class);
        // The machine doesn't know WHICH adapter (REST/JMS) is called.
        depositPort.submitDeposit(request, spec);
    }
}
```

With this step, we have achieved true architectural decoupling. The core domain is now a generic engine that is configured, composed, and directed entirely by the `JourneySpecification`. Adding a new payment flow is now primarily a matter of adding a new entry to `application.yml`, not changing battle-hardened Java code.

---
## Step 6: Refactor the Application Core

### The "Why"

We have built a sophisticated framework of guarantees and dispatchers, but its value is purely theoretical until the application itself adopts it. The existing application core (`Controllers`, `CommandHandlers`, `StateMachines`) is still burdened with the architectural sins of the past: hardcoded dependencies, imperative logic, and implicit assumptions.

This final step is the "harvest." It is the process of methodically migrating the application code onto the new framework, thereby reaping the promised benefits of flexibility, safety, and maintainability. The goal is to transform the core domain into a pristine, generic, and declarative engine, whose components are lean, focused, and ignorant of the specific implementation details they are orchestrating. We are turning our `CommandHandlers` from "doers" into "delegators."

### The "What"

This step involves a systematic, top-to-bottom refactoring of the `book-transfers` service to align with the God-Plan's patterns. We will purge all legacy patterns and replace them with their superior, framework-driven counterparts.

1.  **Refactor Inbound Adapters (`Controllers`):** Their sole responsibility will be reduced to that of an **Anti-Corruption Layer (ACL)**. They will do nothing but translate external DTOs into internal `Command` objects. All other concerns (context setup, syntactic validation) are now handled by the framework's filters and middleware.
2.  **Refactor Use Cases (`CommandHandlers`):** This is the most critical transformation. Handlers will be stripped of all imperative logic and specific dependencies. They will become lean orchestrators that declare their needs via the `@InJourney` annotation and delegate all complex operations to the framework's dispatchers and orchestrators.
3.  **Refactor Outbound Port Interactions (within `StateMachines` or other components):** All direct dependencies on specific port adapters (e.g., `RestDepositAdapter`) will be replaced with dependencies on the generic `Port.Dispatcher` interface. This ensures that the choice of adapter is always deferred to the `JourneySpecification`.

### The "How"

This is a methodical process of identifying anti-patterns in the existing code and replacing them with the new, correct patterns.

#### Sub-step 6.1: Purify the `Controllers`

1.  **Identify:** Locate a `RestController` in the `.../adapter/in/web/` package.
2.  **Analyze:** Observe its current responsibilities. It likely contains logic for validation, context setup, or other concerns.
3.  **Refactor:** Strip the controller method down to its absolute minimum responsibility.

**Before (Overloaded Controller):**

```java
// In .../adapter/in/web/PaymentController.java
@RestController
public class PaymentController {
    private final CommandBus commandBus;
    private final SomeLegacyValidator validator;

    // ... constructor ...

    @PostMapping("/payments")
    public ResponseEntity<Void> submitPayment(@RequestBody PaymentApiRequest request) {
        // Manual validation, context setup, etc.
        validator.validate(request);
        LegacyContextHolder.setContext(request.getJourneyName());
        
        // Manual mapping
        SubmitPaymentCommand command = new SubmitPaymentCommand(request.get...);
        
        commandBus.send(command);
        return ResponseEntity.accepted().build();
    }
}
```

**After (Pristine ACL Controller):**

```java
// In .../adapter/in/web/PaymentController.java
@RestController
public class PaymentController {
    private final CommandBus commandBus;
    private final ApiMapper apiMapper; // Using MapStruct for clean mapping

    // ... constructor ...

    @PostMapping("/payments")
    public ResponseEntity<Void> submitPayment(@RequestBody PaymentApiRequest request) {
        // 1. Map to internal command.
        SubmitPaymentCommand command = apiMapper.toCommand(request);
        
        // 2. Send to the bus. That is all.
        // The JourneyScopeFilter has already set the context.
        // The CommandValidationMiddleware will intercept this call to perform syntactic validation.
        commandBus.send(command);
        
        return ResponseEntity.accepted().build();
    }
}
```

#### Sub-step 6.2: Transform the `CommandHandlers` into Lean Orchestrators

1.  **Identify:** Locate a `CommandHandler` (e.g., `SubmitPaymentCommandHandler`).
2.  **Analyze:** It is likely injected with specific adapters (`@Qualifier`), contains `if/else` logic for different journeys, and performs manual validation or enrichment.
3.  **Refactor:** Aggressively remove all specific dependencies and imperative logic. Re-implement the handler to be a pure delegator to the framework.

**Before (Rigid, Imperative Handler):**

```java
@Component
public class OldSubmitPaymentCommandHandler implements CommandHandler<SubmitPaymentCommand> {
    
    @Autowired @Qualifier("restDepositAdapter")
    private DepositPort restDepositAdapter;

    @Autowired @Qualifier("jmsDepositAdapter")
    private DepositPort jmsDepositAdapter;

    @Autowired
    private CustomerRepository customerRepo;

    @Override
    public void handle(SubmitPaymentCommand command) {
        // Manual data enrichment
        Customer customer = customerRepo.findById(command.getCustomerId());

        // Manual business rule validation
        if (customer.isNotActive()) {
            throw new BusinessException("Customer is not active");
        }
        if (customer.getBalance().isLessThan(command.getAmount())) {
            throw new BusinessException("Insufficient funds");
        }

        // Hardcoded, journey-specific logic
        if ("NEW_ASYNC_JOURNEY".equals(command.getJourneyName())) {
            jmsDepositAdapter.submitDeposit(...);
        } else {
            restDepositAdapter.submitDeposit(...);
        }
    }
}
```

**After (The God-Plan Way - Lean, Declarative Handler):**

```java
@Component
public class SubmitPaymentCommandHandler implements CommandHandler<SubmitPaymentCommand> {

    // Inject the FRAMEWORK components, not specific implementations
    private final DataCollectorOrchestrator dataCollector;
    private final BusinessRuleOrchestrator ruleOrchestrator;
    private final StateMachineFactoryDispatcher stateMachineDispatcher;

    public SubmitPaymentCommandHandler(
        DataCollectorOrchestrator dataCollector,
        BusinessRuleOrchestrator ruleOrchestrator,
        StateMachineFactoryDispatcher stateMachineDispatcher
    ) {
        this.dataCollector = dataCollector;
        this.ruleOrchestrator = ruleOrchestrator;
        this.stateMachineDispatcher = stateMachineDispatcher;
    }

    @Override
    public void handle(
        SubmitPaymentCommand command,
        // Declare dependency on the context and its contract
        @InJourney(requires = StandardPaymentContract.class) JourneySpecification spec
    ) {
        // 1. Delegate Enrichment: The handler doesn't know or care WHICH collectors are run.
        EnrichedCommand enrichedCommand = dataCollector.enrich(command, spec);

        // 2. Delegate Validation: The handler doesn't know or care WHICH rules are run.
        ruleOrchestrator.execute(enrichedCommand, spec)
            .getOrElseThrow(violations -> new BusinessRuleValidationException(violations.toString()));

        // 3. Delegate Orchestration: The handler doesn't know or care WHICH state machine is used.
        StateMachineFactory factory = stateMachineDispatcher.getFactory(spec);
        StateMachine<States, Events> stateMachine = factory.create(enrichedCommand, spec);
        
        stateMachine.start();
    }
}
```

#### Sub-step 6.3: Decouple Outbound Port Interactions

1.  **Identify:** Locate any component that calls an outbound port, such as a `StateMachine` action method.
2.  **Analyze:** Check its dependencies. It should not be injected with a concrete adapter like `RestDepositAdapter`.
3.  **Refactor:** Ensure the dependency is on the `Port.Dispatcher` interface and that the `JourneySpecification` is passed down to the call.

**Before (Tightly Coupled State Machine):**

```java
@WithStateMachine
public class StandardPaymentStateMachine {
    // Wrong: Depends on a specific implementation
    private final RestDepositAdapter depositAdapter; 

    // ...
    private void doSubmitDeposit(StateContext<S, E> context) {
        // ... get request from context ...
        // This call is hardcoded to the REST adapter
        depositAdapter.submitDeposit(request);
    }
}
```

**After (Decoupled State Machine):**

```java
@WithStateMachine
public class StandardPaymentStateMachine {
    // Correct: Depends on the generic dispatcher interface
    private final DepositPort.Dispatcher depositPort;

    public StandardPaymentStateMachine(DepositPort.Dispatcher depositPort, /*...other deps...*/) {
        this.depositPort = depositPort;
    }

    // ...
    private void doSubmitDeposit(StateContext<S, E> context) {
        // Retrieve the spec, which was placed in the context by the CommandHandler
        JourneySpecification spec = context.getExtendedState().get("JourneySpecification", JourneySpecification.class);
        SubmitDepositRequest request = context.getExtendedState().get("DepositRequest", SubmitDepositRequest.class);

        // The machine doesn't know WHICH adapter (REST/JMS) is called.
        // It delegates that decision to the dispatcher, which uses the spec.
        depositPort.submitDeposit(request, spec);
    }
}
```

### The "When"

This is the final, culminating implementation phase of the God-Plan. It is executed after the entire framework (Steps 1-5) is complete. This is the point where the architectural vision, meticulously planned and guaranteed by the framework, is fully realized in the application code.

### Peek of the Implementation

Upon completion of this step, the `book-transfers` service will be a model of modern, configurable software design.

*   **Ultimate Flexibility:** Adding a new payment journey that reuses existing logic but calls a new, third-party credit service via gRPC is now trivial. We would:
    1.  Implement a `GrpcCreditAdapter`.
    2.  Add a new journey definition to `application.yml`, pointing `adapterRouting.creditPort` to the bean name of our new adapter.
    3.  The application now supports the new flow **with zero changes to the core domain logic**.
*   **Fearless Refactoring:** The core logic inside `CommandHandlers` and `StateMachines` is now so simple, high-level, and declarative that it is trivial to read, understand, and maintain.
*   **Architectural Purity:** The Hexagonal Architecture is no longer just a theoretical diagram; it is a living, breathing reality enforced by the framework. The core is pristine and completely isolated from the messy details of the outside world.

---

The God-Plan is complete. The system has reached its pinnacle state.
