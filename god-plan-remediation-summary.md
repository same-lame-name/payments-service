# God-Plan Remediation Summary

**Version:** 1.0
**Status:** Proposed

---

## 1. Objective

This document provides a high-level summary of the proposed solutions for the eight architectural weaknesses identified in `god-plan-critique.md`. Each proposal aims to strengthen the original plan by increasing type safety, reducing complexity, and ensuring adherence to core software engineering principles, thereby forging a more robust and maintainable final architecture.

---

## 2. Remediation Proposals

### 2.1. For: Brittle Reliance on String-Based Reflection

*   **Proposal: Introduce Type-Safe Configuration Bindings.**
*   **Peek:** We will replace string identifiers in YAML with type-safe structures, primarily enums. For example, instead of `journeyContract: "...StandardPaymentContract"`, the YAML will use a logical name like `contract: STANDARD_PAYMENT`. A custom Spring `Converter` or `ConfigurationProperties` binder will map this enum to the `StandardPaymentContract.class` object at boot time. This moves error detection from a runtime `ClassNotFoundException` to a compile-time error if the enum is removed, and a clear boot-time failure if the mapping is invalid. This same pattern will be applied to bean names for adapters, rules, and collectors.

### 2.2. For: Duplicated Contract Validation Logic

*   **Proposal: Centralize Validation in a Single Component.**
*   **Peek:** We will create a single, stateless Spring bean: `JourneyContractValidator`. This component will have one method: `validate(JourneySpecification spec, JourneyContract contract)`. The startup-time `JourneyIntegrityValidator` (Step 2) and the runtime `InJourneyParameterAspect` (Step 3) will both be injected with and delegate all validation logic to this single component. This ensures the validation rules are defined in exactly one place, adhering to the DRY principle.

### 2.3. For: Over-engineered and Unnecessary Custom Rule Engine

*   **Proposal: Replace the Custom Framework with the Strategy Pattern.**
*   **Peek:** We will eliminate the `@BusinessRule`, `@When`, and `@Then` annotations entirely. We will define a simple `BusinessRule` interface (e.g., `interface BusinessRule { ValidationResult execute(Context context); }`). Each rule will be a standard Spring `@Component` implementing this interface. The `BusinessRuleOrchestrator` will be injected by Spring with a `Map<String, BusinessRule>`, mapping bean names to implementations. This approach is transparent, leverages standard Spring features, is easy to debug, and provides superior IDE support for code navigation.

### 2.4. For: Fragile Validation Group Resolution

*   **Proposal: Implement a Dedicated `ValidationGroupRegistry`.**
*   **Peek:** We will create a `ValidationGroupRegistry` bean. At application startup, this bean will scan the `ValidationGroups` marker interface for all its nested classes and build a `Map<String, Class<?>>` (e.g., mapping the string "StandardPayment" to the `ValidationGroups.StandardPayment.class` object). The `CommandValidationMiddleware` will be injected with this registry and use it for safe, reliable, and refactor-proof resolution of validation groups.

### 2.5. For: Inconsistent Component Registry Pattern

*   **Proposal: Enforce Consistent Use of the Generic `ComponentRegistry`.**
*   **Peek:** The `PortDispatcher` implementations will be refactored. They will no longer be injected with a `List` of adapters and the `ApplicationContext`. Instead, they will be injected with the specific `ComponentRegistry` they need (e.g., `ComponentRegistry<DepositPort>`). The responsibility for creating and configuring these registries will be centralized in a single `RegistryConfiguration` class, ensuring a consistent, clean, and reusable pattern across the entire application.

### 2.6. For: Overly Complex `JourneyContract` Mechanism

*   **Proposal: Redesign Contracts to be Declarative Interfaces with a Proxy-Based Validator.**
*   **Peek:** The `JourneyContract` will be simplified. Instead of returning a list of functions, it will be an interface with getter-like methods (e.g., `String depositPort();`). A component needing this contract will be injected with a dynamic proxy that implements this interface. When the component calls `contract.depositPort()`, the proxy's `InvocationHandler` will intercept the call, retrieve the actual value from the underlying `JourneySpecification`, validate it (e.g., check for null/blank), and return it. This makes the contract definition clean, intuitive, and leverages interception over complex boilerplate.

### 2.7. For: Unaddressed Concurrency Risks in Data Collection

*   **Proposal: Define an Immutable, Merge-Based Concurrency Model.**
*   **Peek:** We will redesign the `DataCollector` interface to be a pure function: `EnrichmentData collect(Context context)`. Each collector runs in parallel and returns a small, immutable data object containing only the information it gathered. The `DataCollectorOrchestrator`, after all parallel futures complete, will perform a deterministic, sequential merge of these `EnrichmentData` objects into a single, immutable `EnrichedContext`. This context is then passed to the business rule engine. This design eliminates the possibility of race conditions by construction.

### 2.8. For: Implicit State Machine Contract

*   **Proposal: Introduce a Type-Safe `StateMachineInput` Object.**
*   **Peek:** We will eliminate the use of the `ExtendedState` map for passing the `JourneySpecification`. We will create a new, immutable record: `record StateMachineInput(EnrichedContext context, JourneySpecification spec)`. The `StateMachineFactory`'s `create` method will accept this object. The state machine implementation will be given this strongly-typed object in its constructor, making the dependency explicit and compile-time safe. The `ExtendedState` will be reserved for mutable state generated and used exclusively during the machine's execution.
