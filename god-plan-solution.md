# God-Plan Pinnacle Solution

**Version:** 1.0
**Status:** Proposed

---

## 1. Foreword

This document provides the definitive, unabridged, and robust remediation for each architectural flaw identified in `god-plan-critique.md`. It supersedes the high-level `god-plan-remediation-summary.md` with detailed, actionable, and architecturally pure solutions.

Each proposal herein is designed to be a pillar of the final pinnacle architecture. The solutions are not patches; they are fundamental corrections that increase type safety, enforce consistency, eliminate "magic," and align the implementation with the core principles of Hexagonal Architecture, DDD, and our primary directive: **"Configuration over Code, Guaranteed by the Framework."**

This is the blueprint for the final, correct implementation.

---

## 2. Core Architectural & Philosophical Remediations

### 2.1. For: Flawed Entry Point: Technology-Specific Context Injection

*   **The Flaw:** Tying journey context injection to a web-specific `JourneyScopeFilter` violates technology agnosticism.
*   **The Pinnacle Solution: Generic Middleware-Based Context Injection.**

    The establishment of the journey context must occur at the most generic entry point into our core domain: the `CommandBus` and `QueryBus`. This ensures that regardless of the trigger—be it a REST controller, a Kafka listener, a gRPC service, or a scheduled job—the context is established consistently and without fail. All inbound adapters must ultimately route through the bus, making it the perfect, technology-agnostic chokepoint.

    We will implement this using AOP.

*   **Peek of the Implementation:**

    1.  **Create a `JourneyContextProvider` Aspect:** This aspect will wrap the `send` method of both the `CommandBus` and `QueryBus`.
    2.  **Define a `JourneyIdentifiable` Interface:** Commands and Queries that initiate a journey will implement this interface, which provides a single method: `getJourneyName()`. This creates an explicit, compile-time contract for identifying a journey.
    3.  **Implement the Aspect Logic:** The aspect checks if the command/query implements `JourneyIdentifiable`. If it does, the aspect retrieves the journey name, loads the corresponding `JourneySpecification` from a central registry, and uses the `package-private` `JourneyContext.runWith(...)` method to bind the specification to a `ScopedValue` for the duration of the bus's `send` operation. If the command does not implement the interface, the operation proceeds without a journey context.

    ```java
    // In core/domain/shared/context/JourneyIdentifiable.java
    package dexter.banking.booktransfers.core.domain.shared.context;

    /**
     * An explicit contract for any Command or Query that can initiate a journey.
     */
    public interface JourneyIdentifiable {
        String getJourneyName();
    }

    // In infrastructure/aspect/JourneyContextProviderAspect.java
    package dexter.banking.booktransfers.infrastructure.aspect;

    import dexter.banking.booktransfers.core.domain.shared.context.JourneyContextDeprecated;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneyIdentifiable;
    import dexter.banking.booktransfers.core.domain.shared.context.JourneySpecificationDeprecated;
    import dexter.banking.booktransfers.infrastructure.registry.JourneySpecificationRegistry; // Fetches the spec
    import org.aspectj.lang.ProceedingJoinPoint;
    import org.aspectj.lang.annotation.Around;
    import org.aspectj.lang.annotation.Aspect;
    import org.springframework.stereotype.Component;

    @Aspect
    @Component
    public class JourneyContextProviderAspect {

        private final JourneySpecificationRegistry journeyRegistry;

        public JourneyContextProviderAspect(JourneySpecificationRegistry journeyRegistry) {
            this.journeyRegistry = journeyRegistry;
        }

        @Around("execution(* dexter.banking.booktransfers.core.port.in.CommandBus.send(..)) || " +
                "execution(* dexter.banking.booktransfers.core.port.in.QueryBus.send(..))")
        public Object establishContext(ProceedingJoinPoint pjp) throws Throwable {
            Object commandOrQuery = pjp.getArgs()[0];

            if (!(commandOrQuery instanceof JourneyIdentifiable identifiable)) {
                // Not a journey-initiating object, proceed without context.
                return pjp.proceed();
            }

            String journeyName = identifiable.getJourneyName();
            if (journeyName == null || journeyName.isBlank()) {
                // Proceed without context, or throw an error for an invalid name.
                return pjp.proceed();
            }

            JourneySpecification spec = journeyRegistry.get(journeyName);

            // Bind the spec to a ScopedValue for the entire operation.
            return JourneyContext.runWith(spec, () -> pjp.proceed());
        }
    }
    ```

### 2.2. For: Pervasive Brittleness via String-Based Contracts

*   **The Flaw:** Using raw strings in YAML for bean names, class names, and validation groups is fragile and bypasses compile-time safety.
*   **The Pinnacle Solution: Type-Safe, Enum-Driven Configuration Definitions.**

    We will completely eliminate raw strings for references to code artifacts. The YAML configuration will use logical, type-safe `enum` names. A robust, multi-stage loading process will transform these logical definitions into fully resolved, type-safe `JourneySpecification` objects at boot time. This gives us the best of both worlds: the readability of YAML and the safety of the Java type system.

*   **Peek of the Implementation:**

    1.  **Define Enums:** Create enums for every point of variation: `JourneyContractType`, `AdapterId`, `BusinessRuleId`, `ValidationGroupId`.

    2.  **Redefine YAML:** The `application.yml` will use these enum names, not raw strings.

    3.  **Create a `JourneyDefinition` Record:** This record will be a 1:1 mapping of the raw YAML structure, using our new enums.

    4.  **Implement `JourneySpecificationLoader`:** This boot-time component will be injected with the `JourneyDefinition` properties from Spring. It will then use registries (e.g., a `Map<JourneyContractType, JourneyContract>`) to resolve the enums to their corresponding concrete class instances or bean names, populating the final, fully-typed `JourneySpecification` object. This moves failure from a runtime `NullPointerException` to an immediate and clear boot-time error if a mapping is missing.

    ```java
    // In core/domain/shared/config/JourneyContractType.java (Enum)
    public enum JourneyContractType {
        STANDARD_PAYMENT,
        WALLET_TOPUP
    }

    // In application.yml
    app:
      journeys:
        PAYMENT_SUBMIT_V1:
          contract: STANDARD_PAYMENT // Enum, not a FQCN string
          validation:
            groups: [STANDARD_PAYMENT, WALLET_JOURNEY] // Enums
          adapterRouting:
            depositPort: DEPOSIT_PORT_REST // Enum (AdapterId)
            creditPort: CREDIT_PORT_JMS
            limitPort: LIMIT_PORT_REST
          ...

    // In infrastructure/config/JourneyDefinition.java (Raw mapping)
    @ConfigurationProperties(prefix = "app")
    public record JourneyDefinition(Map<String, JourneyConfig> journeys) {
        public record JourneyConfig(
            JourneyContractType contract,
            ValidationConfig validation,
            AdapterRouting adapterRouting,
            ...
        ) {}
        // ... other nested records using enums
    }

    // In infrastructure/config/JourneySpecificationLoader.java (The resolver)
    @Component
    public class JourneySpecificationLoader {
        // Injected with registries that map enums to beans/classes
        private final Map<JourneyContractType, JourneyContract> contractRegistry;
        private final Map<AdapterId, String> adapterBeanNameRegistry;

        public Map<String, JourneySpecification> load(JourneyDefinition definition) {
            // For each journey in the definition:
            // 1. Resolve contractType enum to a JourneyContract object.
            // 2. Resolve adapterId enums to their Spring bean names (String).
            // 3. Construct the final, fully-typed JourneySpecification.
            // This fails fast at boot if any enum cannot be resolved.
        }
    }
    ```

### 2.3. For: Non-Generic and Unscalable Integrity Validation

*   **The Flaw:** The `JourneyIntegrityValidator` is a centralized bottleneck with hardcoded validation paths.
*   **The Pinnacle Solution: A Decentralized, Self-Validating Configuration Graph.**

    The responsibility for validation should not belong to a single, monolithic component. Instead, the `JourneySpecification` itself will become a graph of objects, where each object that references a configurable component is responsible for its own validation. We will introduce a `Validatable` interface that these configuration objects will implement.

*   **Peek of the Implementation:**

    1.  **Define `Validatable` Interface:** `interface Validatable { void validate(String journeyName, ApplicationContext context); }`

    2.  **Refine Configuration Records:** The nested records within `JourneySpecification` (like `AdapterRouting`) will implement `Validatable`. Their `validate` method will contain the logic to check for the existence of the beans they reference.

    3.  **Simplify `JourneyIntegrityValidator`:** The central validator's job is now trivial. It simply iterates through the loaded `JourneySpecification` objects and recursively calls `validate()` on any field that implements the `Validatable` interface. Adding a new configurable component now only requires that its configuration record implements the `Validatable` interface; the central validator requires no changes.

    ```java
    // In core/domain/shared/config/Validatable.java
    public interface Validatable {
        void validate(String journeyName, ApplicationContext context)
            throws IllegalStateException;
    }

    // In core/domain/shared/context/JourneySpecification.java
    public record JourneySpecification(
        // ...
        AdapterRouting adapterRouting, // This record will implement Validatable
        // ...
    ) {
        public record AdapterRouting(
            String depositPort, // These are now bean names, resolved from enums
            String creditPort,
            String limitPort
        ) implements ValueObject, Validatable {
            @Override
            public void validate(String journeyName, ApplicationContext context) {
                ensureBeanExists(depositPort, "adapterRouting.depositPort", journeyName, context);
                ensureBeanExists(creditPort, "adapterRouting.creditPort", journeyName, context);
                ensureBeanExists(limitPort, "adapterRouting.limitPort", journeyName, context);
            }

            private void ensureBeanExists(...) { /* ... */ }
        }
    }

    // In infrastructure/adapter/in/config/JourneyIntegrityValidator.java
    public class JourneyIntegrityValidator implements ApplicationListener<ContextRefreshedEvent> {
        // ...
        private void validateJourney(JourneySpecification spec) {
            // Use reflection to find all fields that are Validatable and call validate()
            for (Field field : spec.getClass().getDeclaredFields()) {
                if (Validatable.class.isAssignableFrom(field.getType())) {
                    Validatable validatable = (Validatable) field.get(spec);
                    validatable.validate(spec.journeyName(), applicationContext);
                }
            }
        }
    }
    ```

---

## 3. Component-Level Design Remediations

### 3.1. For: Over-engineered and Unnecessary Custom Rule Engine

*   **The Flaw:** The proposed `@BusinessRule`, `@When`, `@Then` framework is complex, magical, and hard to debug.
*   **The Pinnacle Solution: The Standard, Type-Safe Strategy Pattern.**

    We will replace the bespoke annotation framework with the simple, powerful, and transparent Strategy design pattern. Each business rule will be a standard Spring component that implements a common `BusinessRule` interface. This approach is debug-friendly, provides full IDE navigation and refactoring support, and leverages the container's dependency injection capabilities without any reflective "magic."

*   **Peek of the Implementation:**

    1.  **Define `BusinessRule` Interface:** A simple functional interface.
    2.  **Implement Rules as Strategies:** Each rule is a `@Component` implementing the interface.
    3.  **Use an Enum-Keyed Map:** The `BusinessRuleOrchestrator` will be injected by Spring with a `Map<BusinessRuleId, BusinessRule>`, where `BusinessRuleId` is the enum from solution 2.2. This provides type-safe, efficient lookup.

    ```java
    // In core/domain/shared/rule/BusinessRule.java
    public interface BusinessRule {
        Validation<String, EnrichedContext> execute(EnrichedContext context);
    }

    // In infrastructure/rule/SufficientFundsRule.java
    @Component
    public class SufficientFundsRule implements BusinessRule {
        @Override
        public Validation<String, EnrichedContext> execute(EnrichedContext context) {
            // No @When/@Then magic. Just standard Java.
            if (context.getAccountBalance() == null) {
                return Validation.valid(context); // Rule is not applicable
            }
            if (context.getAccountBalance().compareTo(context.getAmount()) >= 0) {
                return Validation.valid(context);
            } else {
                return Validation.invalid("Insufficient funds.");
            }
        }
    }

    // In infrastructure/orchestration/BusinessRuleOrchestrator.java
    @Component
    public class BusinessRuleOrchestrator {
        private final Map<BusinessRuleId, BusinessRule> ruleRegistry;

        // Spring automatically populates this map from all BusinessRule beans
        // that have a bean name matching a BusinessRuleId enum value.
        public BusinessRuleOrchestrator(Map<BusinessRuleId, BusinessRule> ruleRegistry) {
            this.ruleRegistry = ruleRegistry;
        }

        public Validation<String, EnrichedContext> execute(EnrichedContext context, JourneySpecification spec) {
            for (BusinessRuleId ruleId : spec.businessRules()) {
                BusinessRule rule = ruleRegistry.get(ruleId);
                // ... execute and fail-fast ...
            }
        }
    }
    ```

### 3.2. For: Overly Complex and Boilerplate-Heavy `JourneyContract`

*   **The Flaw:** The `getRequiredConfigAccessors()` method is obtuse and developer-hostile.
*   **The Pinnacle Solution: Declarative Interfaces with a Dynamic Proxy Validator.**

    The `JourneyContract` will become a simple, declarative Java interface with getter-style methods. A component needing a contract will be injected with a dynamic proxy that implements this interface. The proxy's `InvocationHandler` will intercept method calls, look up the corresponding value in the *actual* `JourneySpecification`, perform the required validation (e.g., non-null), and return the value. This makes the contract clean, the intent clear, and the validation logic centralized and invisible to the consumer.

*   **Peek of the Implementation:**

    1.  **Simplify `JourneyContract`:** It becomes a clean interface.
    2.  **Create `JourneyContractProxyFactory`:** This factory creates a JDK dynamic proxy for a given contract interface.
    3.  **Implement `ContractInvocationHandler`:** This is the core logic. It intercepts calls, maps the method name to a path in the `JourneySpecification`, retrieves the value, validates it, and returns it. This provides runtime safety.

    ```java
    // In core/domain/shared/contract/StandardPaymentContract.java
    public interface StandardPaymentContract extends JourneyContract {
        // The contract is now a clean, declarative interface.
        // The method name implies the required data.
        AdapterRouting adapterRouting();
    }

    // In infrastructure/contract/ContractInvocationHandler.java (Conceptual)
    public class ContractInvocationHandler implements InvocationHandler {
        private final JourneySpecification spec;

        // ... constructor ...

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 1. Map method name "adapterRouting" to spec.adapterRouting()
            //    This can be done via convention or a more robust mapping.
            Object value = spec.adapterRouting(); // Simplified for example

            // 2. Validate the retrieved value (e.g., check for null)
            if (value == null) {
                throw new ContractViolationException(
                    String.format("Journey '%s' violates contract. Required config '%s' is missing.",
                        spec.journeyName(), method.getName()));
            }

            // 3. Return the validated value.
            return value;
        }
    }

    // Usage in a CommandHandler
    public void handle(SubmitPaymentCommand command, @InJourney JourneySpecification spec) {
        // The framework provides the proxy, not the raw spec.
        StandardPaymentContract contract = JourneyContractProxyFactory.create(spec, StandardPaymentContract.class);

        // This call is intercepted by the invocation handler.
        // It is guaranteed to return a non-null value or throw a ContractViolationException.
        AdapterRouting routing = contract.adapterRouting();
    }
    ```

### 3.3. For: Unsafe Concurrency Model in `DataCollectorOrchestrator`

*   **The Flaw:** The concurrency model was undefined and assumed thread-safety.
*   **The Pinnacle Solution: An Immutable, Functional, Merge-Based Concurrency Model.**

    We will enforce safety by construction. `DataCollector`s will be pure functions that accept a context and return a new, immutable data object containing only the information they gathered. The orchestrator will execute these in parallel and then perform a deterministic, sequential merge of the results into a final, immutable `EnrichedContext`. This design has no shared mutable state and is therefore inherently free of race conditions.

*   **Peek of the Implementation:**

    ```java
    // In core/port/in/enrichment/DataCollector.java
    public interface DataCollector {
        /**
         * A pure function that collects data and returns an immutable result.
         * It must not mutate the input command.
         */
        EnrichmentFragment collect(SubmitPaymentCommand command);
    }

    // In core/domain/shared/enrichment/EnrichmentFragment.java
    // A simple marker interface for data fragments.
    public interface EnrichmentFragment {}
    public record CustomerProfileFragment(CustomerProfile profile) implements EnrichmentFragment {}
    public record AccountBalanceFragment(BigDecimal balance) implements EnrichmentFragment {}

    // In infrastructure/orchestration/DataCollectorOrchestrator.java
    public class DataCollectorOrchestrator {
        // ... registry of collectors ...

        public EnrichedCommand enrich(SubmitPaymentCommand command, JourneySpecification spec) {
            List<CompletableFuture<EnrichmentFragment>> futures = spec.dataCollectors().stream()
                .map(registry::get)
                .map(collector -> CompletableFuture.supplyAsync(() -> collector.collect(command)))
                .toList();

            // Wait for all parallel collections to complete.
            List<EnrichmentFragment> fragments = futures.stream().map(CompletableFuture::join).toList();

            // Perform a sequential, deterministic merge.
            EnrichedCommand.Builder builder = EnrichedCommand.builder(command);
            for (EnrichmentFragment fragment : fragments) {
                // Use pattern matching to merge safely.
                switch (fragment) {
                    case CustomerProfileFragment f -> builder.customerProfile(f.profile());
                    case AccountBalanceFragment f -> builder.accountBalance(f.balance());
                    // ...
                }
            }
            return builder.build();
        }
    }
    ```

### 3.4. For: Implicit and Fragile State Machine Contract

*   **The Flaw:** Passing the `JourneySpecification` via the untyped `ExtendedState` map is fragile.
*   **The Pinnacle Solution: A Type-Safe `StateMachineInput` Object.**

    We will eliminate the use of the `ExtendedState` map for passing foundational context. A new, immutable record, `StateMachineInput`, will be created to explicitly carry the `EnrichedContext` and the `JourneySpecification`. The `StateMachineFactory` will accept this object, and the state machine implementation will receive it in its constructor. This makes the dependency explicit, visible, and compile-time safe.

*   **Peek of the Implementation:**

    ```java
    // In core/domain/payment/fsm/StateMachineInput.java
    public record StateMachineInput(
        EnrichedCommand command,
        JourneySpecification spec
    ) {}

    // In core/port/out/fsm/StateMachineFactory.java
    public interface StateMachineFactory {
        StateMachine<States, Events> create(StateMachineInput input);
    }

    // In CommandHandler
    public void handle(SubmitPaymentCommand command, @InJourney JourneySpecification spec) {
        // ... enrichment and business rule validation ...
        EnrichedCommand enrichedCommand = ...;

        StateMachineFactory factory = stateMachineDispatcher.getFactory(spec);
        StateMachineInput input = new StateMachineInput(enrichedCommand, spec);

        // Pass the explicit, type-safe input object.
        StateMachine<States, Events> stateMachine = factory.create(input);
        stateMachine.start();
    }

    // In the State Machine implementation
    @WithStateMachine
    public class StandardPaymentStateMachine extends AbstractStateMachine<States, Events> {
        private final StateMachineInput input; // Dependency is now explicit and typed

        public StandardPaymentStateMachine(StateMachineInput input, /* other deps */) {
            this.input = input;
            // ...
        }

        private void someAction(StateContext<States, Events> context) {
            // Access the spec safely from the final field.
            JourneySpecification spec = input.spec();
            // No more error-prone lookups in ExtendedState.
        }
    }
    ```

---

## 4. Implementation, Consistency, and Encapsulation Remediations

### 4.1. For: Duplicated Contract Validation Logic

*   **The Flaw:** Validation logic was repeated in the startup validator and the runtime aspect.
*   **The Pinnacle Solution: A Centralized, Stateless `JourneyContractValidator` Bean.**

    We will create a single, stateless Spring bean, `JourneyContractValidator`, responsible for all contract validation. Both the boot-time `JourneyIntegrityValidator` and the runtime `InJourneyParameterAspect` will be injected with and delegate to this single component, ensuring that the rules are defined in exactly one place.

*   **Peek of the Implementation:**

    ```java
    // In infrastructure/contract/JourneyContractValidator.java
    @Component
    public class JourneyContractValidator {
        public void validate(JourneySpecification spec, JourneyContract contract) throws ContractViolationException {
            // Contains the single, canonical implementation of the validation logic
            // (e.g., using the proxy-based approach from 3.2).
            // It will check all required fields defined by the contract.
        }
    }

    // In JourneyIntegrityValidator (Startup)
    // ... injected with JourneyContractValidator ...
    private void validateJourney(JourneySpecification spec) {
        JourneyContract contract = contractRegistry.get(spec.getContractType());
        contractValidator.validate(spec, contract); // Delegate
    }

    // In InJourneyParameterAspect (Runtime)
    // ... injected with JourneyContractValidator ...
    public Object injectJourneyContext(ProceedingJoinPoint pjp) throws Throwable {
        // ...
        JourneyContract requiredContract = ...;
        contractValidator.validate(spec, requiredContract); // Delegate
        // ...
    }
    ```

### 4.2. For: Inconsistent Component Registry Pattern

*   **The Flaw:** The `DepositPortDispatcher` implemented its own manual registration logic, ignoring the generic `ComponentRegistry`.
*   **The Pinnacle Solution: Enforce Universal Adoption of the Generic `ComponentRegistry`.**

    All dispatcher components will be refactored to use the single, consistent `ComponentRegistry` pattern. They will be injected with a pre-populated, type-safe registry, not a raw `List` of beans and the `ApplicationContext`. This centralizes responsibility and simplifies the dispatchers.

*   **Peek of the Implementation:**

    ```java
    // In infrastructure/config/RegistryConfiguration.java
    @Configuration
    public class RegistryConfiguration {
        // Centralized creation of all registries.
        @Bean
        public ComponentRegistry<DepositPort> depositPortRegistry(ApplicationContext context) {
            // The registry filters out the dispatcher itself to prevent recursion.
            return new ComponentRegistry<>(DepositPort.class, context, port -> !(port instanceof DepositPort.Dispatcher));
        }
        // ... other registries
    }

    // In infrastructure/adapter/out/deposit/DepositPortDispatcher.java
    @Component("depositPortDispatcher")
    public class DepositPortDispatcher implements DepositPort.Dispatcher {

        // Injected with the pre-configured registry, not a List.
        private final ComponentRegistry<DepositPort> registry;

        public DepositPortDispatcher(ComponentRegistry<DepositPort> registry) {
            this.registry = registry;
        }

        @Override
        public DebitLegResult submitDeposit(SubmitDepositRequest request, JourneySpecification spec) {
            String adapterBeanName = spec.adapterRouting().depositPort();
            DepositPort adapter = registry.get(adapterBeanName); // Simple, clean lookup.
            return adapter.submitDeposit(request, spec);
        }
    }
    ```

### 4.3. For: Poor Encapsulation and Configuration Redundancy

*   **The Flaw:** `JourneyContext` was a `public` class with no public members, and the journey name was duplicated in the YAML.
*   **The Pinnacle Solution: Enforce Strict Encapsulation and DRY in Configuration.**

    1.  **`JourneyContext` Encapsulation:** The `JourneyContext` utility class will be made `package-private` and `final`. It is a framework-internal implementation detail and must not be part of the core domain's public API.
    2.  **YAML DRY:** The journey name will only be used as the key in the YAML map. The `JourneySpecificationLoader` (from solution 2.2) will be responsible for taking this key and populating it as a field on the `JourneySpecification` object during the loading process.

*   **Peek of the Implementation:**

    ```java
    // In core/domain/shared/context/JourneyContext.java
    package dexter.banking.booktransfers.core.domain.shared.context;

    import java.util.concurrent.Callable;

    /**
     * A sealed, framework-internal utility. Not for application developer use.
     */
    final class JourneyContext { // Now package-private and final
        // ... same internal implementation
    }

    // In application.yml
    app:
      journeys:
        PAYMENT_SUBMIT_V1: # Name is ONLY the key
          contract: STANDARD_PAYMENT
          # No redundant 'journeyName: "PAYMENT_SUBMIT_V1"' property
          ...

    // In infrastructure/config/JourneySpecificationLoader.java
    public Map<String, JourneySpecification> load(JourneyDefinition definition) {
        return definition.journeys().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    String journeyName = entry.getKey(); // Get name from key
                    JourneyConfig config = entry.getValue();
                    // ... resolve all enums and other properties ...
                    return new JourneySpecification(
                        journeyName, // Populate the name here
                        resolvedContract,
                        resolvedAdapters,
                        // ...
                    );
                }
            ));
    }
    ```