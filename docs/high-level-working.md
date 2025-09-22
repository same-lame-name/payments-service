### The Grand Design: A High-Level Architectural Overview

This document provides the complete, high-level overview of our system's architecture. It serves as the single source of truth for the core architectural patterns and the reasoning behind them, ensuring that local implementation decisions always align with the grand design.

Our architecture is built on four foundational pillars that work together to create a system that is robust, decoupled, maintainable, and safe.

--- 

### Pillar 1: The Anti-Corruption Layer (ACL)

*   **What:** A strict separation between the external world (`ApiRequest` DTOs) and our internal domain (`Command` objects). The external DTO mirrors the API specification, while the internal `Command` is a pure, purpose-built object representing a specific business intent.

*   **Why:** To decouple our core domain from the volatility of external contracts. The API specification can change without forcing immediate changes to our internal business logic. This layer protects the domain from messy, untrusted data and translates it into a clean, trusted, and immutable internal representation.

*   **When:** Immediately upon receiving a request.

*   **How:** The `Controller` is responsible for this translation. It receives the raw `ApiRequest` DTO from the request body. It then immediately uses a compile-time safe mapping library (e.g., MapStruct) to translate the `ApiRequest` into a specific, clean `Command` object. This `Command` is then passed to the `CommandBus`.

--- 

### Pillar 2: Context-Aware Syntactic Validation

*   **What:** A declarative framework for validating the incoming `Command` based on the context of the current journey. This uses the standard Java Bean Validation (JSR 303) framework, enhanced with **Validation Groups**.

*   **Why:** To handle complex, context-dependent validation rules (e.g., a field is mandatory for Journey A but optional for Journey B) without writing complex `if/else` logic. This separates the *definition* of a validation rule from its *application*.

*   **When:** In a dedicated `CommandValidationMiddleware`, after the `Command` has been created and after the `JourneySpecification` has been loaded, but before the `Command` reaches the `CommandHandler`.

*   **How:**
    1.  **Definition (The "What"):** We define validation rules (`@NotNull`, `@EitherOr`, etc.) on the fields of our internal `Command` objects. Each rule is assigned to a specific `ValidationGroups` interface (e.g., `@NotNull(groups = WalletJourney.class)`). These annotations are dormant by default.
    2.  **Application (The "When"):** The `JourneySpecification` in `application.yml` contains a `validation-groups` list, which declaratively states which rule groups to activate for that journey.
    3.  **Enforcement:** The `CommandValidationMiddleware` intercepts the `Command`. It reads the active groups from the `JourneySpecification` and invokes the standard `Validator`, telling it to enforce only the rules on the `Command` that belong to the active groups.

--- 

### Pillar 3: Runtime Contract Enforcement

*   **What:** A fail-fast mechanism to guarantee that a `CommandHandler` is never executed for a journey that does not provide the necessary configuration (e.g., required port adapters).

*   **Why:** To prevent runtime errors caused by a mismatch between a handler's operational needs and the journey's configured capabilities. This provides a final, semantic guarantee of safety at the point of execution.

*   **When:** At the last possible moment, just before the `CommandHandler.handle()` method is invoked.

*   **How:** This is achieved non-invasively using **Aspect-Oriented Programming (AOP)**.
    1.  **`JourneyContract`:** We define pure interfaces (e.g., `StandardPaymentContract`) that declare the required ports for a specific type of operation.
    2.  **`@RequiresContract` Annotation:** A developer decorates a `CommandHandler.handle()` method with `@RequiresContract(StandardPaymentContract.class)`.
    3.  **`ContractValidationAspect`:** A "before" advice aspect intercepts the call to `handle`. It compares the contract required by the annotation with the contract declared in the current `JourneySpecification` (retrieved from a `ScopedValue`). If they are not compatible (`isAssignableFrom`), it throws a `ContractViolationException` and halts execution.

--- 

### Pillar 4: Startup-Time Configuration Integrity

*   **What:** A comprehensive validation process that runs once when the application boots to ensure the entire configuration is valid and self-consistent.

*   **Why:** To eliminate an entire class of runtime errors caused by typos, misconfigurations, or incomplete setup in `application.yml`. This guarantees the application cannot start in a broken state.

*   **When:** During Spring context initialization, before the application is ready to accept requests.

*   **How:** A dedicated `JourneyIntegrityValidator` component scans all journey configurations and performs three critical checks:
    1.  **Component Existence:** Verifies that every bean name referenced in the YAML (for port adapters, data collectors, etc.) corresponds to an actual, existing Spring bean.
    2.  **Validation Group Existence:** Verifies that every string listed in a `validation-groups` list corresponds to a real `ValidationGroups` interface defined in the code.
    3.  **Contract Conformance:** Verifies that each journey's configuration provides all the necessary fields required by its declared `JourneyContract`.

Any failure at this stage throws an exception and prevents the application from starting, providing immediate and clear feedback on configuration errors.
