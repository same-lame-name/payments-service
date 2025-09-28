# The God-Plan: A Unified Architectural Blueprint

**Version:** 2.1
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

        @Bean
        @ConfigurationProperties(prefix = "app")
        public Map<String, Map<String, Object>> rawJourneyConfigs() {
            return new java.util.HashMap<>();
        }

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
    }
    ```

#### Sub-step 2.2: Implement the `BlueprintProxyFactory`

1.  **File Location:** `C:/Users/tssha/OneDrive/Desktop/Playground/payments-service/book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/blueprint/BlueprintProxyFactory.java`
2.  **Action:** Create the factory that builds the dynamic proxies using `java.lang.reflect.Proxy`.

    ```java
    // ... (Implementation as previously defined) ...
    ```

---

## Step 3: Implement Guarantee Layer 2 - Technology-Agnostic Context Injection

### The "Why"

Our architecture is critically dependent on the journey context being available to any component that needs it, regardless of the entry point (HTTP, Kafka, Scheduler). Tying context establishment to a web-specific component like a `ServletFilter` is a critical design error that violates technology agnosticism. The context must be established at the most generic entry points to the core: the Command and Query buses.

### The "What"

We will implement a unified, technology-agnostic context injection framework. This consists of a single, secure way to **access** the context, and a minimal set of ways to **establish** it.

1.  **The `@InJourney` Annotation (Access):** The developer's declarative request for the framework to inject the appropriate, type-safe `JourneyBlueprint` proxy. This is the *only* sanctioned way to access the context.
2.  **The `JourneyContext` Utility (Mechanism):** A sealed, `package-private` utility that binds the `JourneySpecification` to a `java.lang.ScopedValue` for the duration of an operation.
3.  **The `InJourneyParameterAspect` (Mechanism):** A lightweight, compile-time woven aspect that retrieves the blueprint from the `JourneyContext` and injects it into an `@InJourney` parameter.
4.  **The `JourneyContextMiddleware` (Establishment):** A mandatory middleware aspect that wraps the `CommandBus` to establish the journey context for all state-changing operations.
5.  **The `@WithJourneyContext` Annotation (Establishment):** A declarative way to establish context for non-command entry points, such as query handlers or internal event listeners.

### The "How"

#### Sub-step 3.1: Define Core Access Contracts (`@InJourney`, `JourneyContext`)

These components are the heart of the secure access pattern and are unchanged from our previous correct design.

1.  **`@InJourney`:** A simple `PARAMETER`-level marker annotation.
2.  **`JourneyContext`:** A `final` class with `package-private` `runWith()` and `getSpec()` methods that manage the `ScopedValue`.

#### Sub-step 3.2: Implement the Command Bus Middleware

This is the primary, technology-agnostic entry point for establishing context.

1.  **Define `JourneyAwareCommand`:** Create a simple interface that all commands requiring a journey context must implement.
    *   **File Location:** `.../core/port/in/JourneyAwareCommand.java`
    ```java
    public interface JourneyAwareCommand {
        String getJourneyName();
    }
    ```

2.  **Implement `JourneyContextMiddleware`:** Create an aspect that wraps the `CommandBus`.
    *   **File Location:** `.../infrastructure/middleware/JourneyContextMiddleware.java`
    ```java
    @Aspect
    @Component
    public class JourneyContextMiddleware {
        private final Map<String, JourneySpecification> journeySpecifications;

        // ... constructor ...

        @Around("execution(* dexter.banking.booktransfers.core.port.in.CommandBus.send(..)) && args(command)")
        public Object establishContextForCommand(ProceedingJoinPoint pjp, Object command) throws Throwable {
            if (!(command instanceof JourneyAwareCommand jc)) {
                return pjp.proceed(); // Proceed without context
            }

            JourneySpecification spec = journeySpecifications.get(jc.getJourneyName());
            if (spec == null) {
                throw new IllegalStateException("Invalid journey name in command: " + jc.getJourneyName());
            }

            // Bind the rich, pre-validated JourneySpecification object for the duration of the call.
            return JourneyContext.runWith(spec, () -> (Object) pjp.proceed());
        }
    }
    ```

#### Sub-step 3.3: Implement the Ad-Hoc Context Annotator

This provides a flexible entry point for queries and other scenarios.

1.  **Define `@WithJourneyContext`:**
    *   **File Location:** `.../core/domain/shared/context/WithJourneyContext.java`
    ```java
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WithJourneyContext {
        String journeyName(); // A SpEL expression
    }
    ```

2.  **Implement `WithJourneyContextAspect`:**
    *   **File Location:** `.../infrastructure/aspect/WithJourneyContextAspect.java`
    ```java
    // Conceptual implementation
    @Aspect
    @Component
    public class WithJourneyContextAspect {
        // ... constructor with journeySpecifications map and SpEL parser ...

        @Around("@annotation(withJourneyContext)")
        public Object establishAdHocContext(ProceedingJoinPoint pjp, WithJourneyContext withJourneyContext) throws Throwable {
            String journeyName = parseSpelExpression(withJourneyContext.journeyName(), pjp.getArgs());
            JourneySpecification spec = journeySpecifications.get(journeyName);
            // ... error handling ...

            return JourneyContext.runWith(spec, () -> (Object) pjp.proceed());
        }
    }
    ```

#### Sub-step 3.4: The Lightweight `InJourneyParameterAspect`

This component is unchanged. It remains a stateless, compile-time woven aspect whose only job is to retrieve the blueprint from the established `JourneyContext` and inject it.

---

## Step 4: Implement Guarantee Layer 3 - The Two-Phase Validation Framework

(This step is refactored to use the `JourneyBlueprint`)

### The "Why"

(Unchanged: Syntactic vs. Semantic validation)

### The "What"

1.  **Phase 1: The Blueprint-Aware Syntactic Validation Framework.** A `CommandValidationMiddleware` uses JSR 303 validation groups read directly from the type-safe `JourneyBlueprint`.
2.  **Phase 2: The Declarative Business Rule Engine.** A simple, strategy-pattern-based rule engine executes a list of rule beans specified in the `JourneyBlueprint`.

### The "How"

#### Sub-step 4.1: Implement the Syntactic Validation Framework

1.  **Define `ValidationGroup` Enum:** A type-safe enum mapping names to group interfaces.

2.  **Implement `CommandValidationMiddleware`:** This middleware is now cleaner and safer.
    *   **Action:** It intercepts the command, gets the list of active group enums from the injected `JourneyBlueprint` (`blueprint.validationGroups()`), converts them to `Class` objects, and invokes the validator.

    ```java
    // In CommandValidationMiddleware.java
    @Before("execution(* ...CommandBus.send(..)) && args(command)")
    public void validateCommand(JoinPoint jp, Object command, @InJourney JourneyBlueprint blueprint) {
        List<ValidationGroup> activeGroups = blueprint.validationGroups();
        if (activeGroups.isEmpty()) return;

        Set<Class<?>> groupClasses = activeGroups.stream()
            .map(ValidationGroup::getGroupClass)
            .collect(Collectors.toSet());

        Set<ConstraintViolation<Object>> violations = validator.validate(command, groupClasses.toArray(new Class[0]));
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
    ```

#### Sub-step 4.2: Implement the Semantic Business Rule Engine

(Unchanged from previous correct design: A simple `BusinessRule` interface, concrete rule beans, and a `BusinessRuleOrchestrator` that uses a `ComponentRegistry` to execute the list of bean names from `blueprint.businessRules()`.)

---

## Step 5: Build the Orchestration Layer - The Blueprint-Driven Engine

(This step is refactored to use the `JourneyBlueprint`)

### The "Why"

(Unchanged: Eliminate rigidity by inverting dependency.)

### The "What"

We will use a combination of the `JourneyBlueprint` proxy and a simplified **"Orchestrator/Registry" pattern**.

1.  **Direct Access via Blueprint:** For single, specific components (e.g., an outbound port adapter), the blueprint provides direct, type-safe access. The complex "Dispatcher" pattern is obsolete.
2.  **Generic Orchestrators:** For executing a *list* of components (e.g., `DataCollectors`), an orchestrator reads the list of bean names from the blueprint and uses a `ComponentRegistry` to execute them.

### The "How"

#### Sub-step 5.1: Implement the `DataCollectorOrchestrator`

*   **Action:** The orchestrator uses a `ComponentRegistry` to execute the list of collector bean names retrieved from `blueprint.dataCollectors()`.

#### Sub-step 5.2: Eliminate Dispatchers in Favor of Direct Blueprint Access

The complex `Port.Dispatcher` pattern is entirely obsolete and should be purged. Access to specific adapters is now direct, type-safe, and compile-time checked via the blueprint.

---

## Step 6: Refactor the Application Core

(This step is refactored to use the `JourneyBlueprint`)

### The "Why"

(Unchanged: Harvest the benefits of the new framework.)

### The "What"

(Unchanged: Refactor Controllers, CommandHandlers, and StateMachines.)

### The "How"

#### Sub-step 6.1: Purify the `Controllers`

(Unchanged: Controller is a pure ACL.)

#### Sub-step 6.2: Transform the `CommandHandlers` into Lean Orchestrators

*   **Action:** The handler is injected with framework orchestrators and the specific `JourneyBlueprint` it requires via `@InJourney`. It delegates all operations.

    ```java
    // In SubmitPaymentCommandHandler.java
    @Override
    public void handle(SubmitPaymentCommand command, @InJourney StandardPaymentBlueprint blueprint) {
        EnrichedCommand enriched = dataCollector.enrich(command, blueprint);
        ruleOrchestrator.execute(enriched, blueprint).getOrElseThrow(...);

        // Get the state machine factory DIRECTLY from the blueprint.
        StateMachineFactory<SubmitPaymentCommand> factory = blueprint.orchestration().engine();
        StateMachine<States, Events> stateMachine = factory.create(enriched, blueprint); // Pass blueprint down
        stateMachine.start();
    }
    ```

#### Sub-step 6.3: Decouple Outbound Port Interactions

This refactoring is now clean, safe, and consistent.

**After (Decoupled State Machine via Blueprint):**

```java
@WithStateMachine
public class StandardPaymentStateMachine {
    // Correct: No dependency on any specific adapter or dispatcher.

    // The action method declares its need for the blueprint via the sanctioned mechanism.
    // The framework's aspect will automatically inject it from the established context.
    private void doSubmitDeposit(
        StateContext<S, E> context,
        @InJourney StandardPaymentBlueprint blueprint
    ) {
        // The blueprint is NOT retrieved from ExtendedState. It is injected directly.
        SubmitDepositRequest request = context.getExtendedState().get("DepositRequest", SubmitDepositRequest.class);

        // The machine doesn't know WHICH adapter (REST/JMS) is called.
        // It gets the correct one directly from the blueprint proxy.
        // This is a COMPILE-TIME SAFE method call!
        DepositPort adapter = blueprint.adapterRouting().depositPort();
        adapter.submitDeposit(request);
    }
}
```

---

The God-Plan is complete. The system has reached its pinnacle state.
