
# Architectural Implementation Specification: The Implicit Context Framework

---
## 1. Grand Design (Active & Approved)

### 1.1. Objective
To establish a universal, implicit mechanism for accessing journey-specific configuration (`JourneyContext`) anywhere in the application, without cluttering method signatures. The framework must be secure, robust, clean, and aligned with the future of the Java platform.

### 1.2. Core Components & Architecture
The architecture is a SpEL-driven, `ScopedValue`-based Implicit Context Framework, implemented with Spring AOP.

*   **`@BeginJourney(String value)` (The Marker)**: A custom annotation placed in the `core` module. It marks a method as the entry point of a business journey. Its `value` attribute contains a SpEL expression to extract the `journeyName` from the method's arguments.

*   **`JourneyContext` (The Carrier)**: An immutable record in the `core` module that holds the `JourneySpecification` for the currently executing journey.

*   **`JourneyContextManager` (The Manager)**: A final class in the `core` module that provides a clean, application-level API for running operations within a specific journey's scope.

*   **`BeginJourneyAspect` (The Engine)**: A Spring AOP Aspect in the `infrastructure` module. It intercepts methods annotated with `@BeginJourney`, uses a SpEL parser to resolve the `journeyName`, fetches the `JourneySpecification` via the existing `ConfigurationPort`, and correctly manages the `ScopedValue` lifecycle.

*   **Build & Dependencies**: The project will be configured using Maven to compile and run on JDK 21+ with `--enable-preview`. The `spring-boot-starter-aop` dependency is required.

---
## 2. MVP 1: Build and Integrate the Core Framework (Complete)

**Objective**: To implement the complete Implicit Context Framework and apply it to a single, representative inbound adapter to prove its end-to-end functionality. This MVP validates the new framework by making the legacy `ConfigurationEnrichmentMiddleware` a client of the new `JourneyContextManager`, as a transitional step.

---
## 3. MVP 2: Decommission the Legacy Context Middleware (Pending)

**Objective**: To fully purge the legacy, `ThreadLocal`-based context system (`ConfigurationEnrichmentMiddleware`, `CommandProcessingContextHolder`, and direct `ConfigurationPort` usage by application components) from the `book-transfers` service. This will establish the `JourneyContextManager` and `@BeginJourney` aspect as the sole, authoritative, and future-compatible mechanism for context management.

**Justification**: The `ConfigurationEnrichmentMiddleware` and its associated `CommandProcessingContextHolder` are architectural debt. They are not compatible with virtual threads, are limited to the command stack, and represent the exact problem the new Implicit Context Framework was designed to solve. Their removal is the ultimate goal of this iteration, leading to a cleaner, more robust, and more maintainable codebase.

**Key Principles for this MVP**:
*   **No Regression**: Ensure all functionality is preserved or improved.
*   **Incremental Change**: Each sub-stage is designed to be small, verifiable, and reversible.
*   **Architectural Purity**: Enforce Hexagonal Architecture by ensuring the `core` module remains free of infrastructure concerns and depends only on its own contracts.
*   **Transparency**: The context should be implicitly available where needed, without explicit parameter passing.

### Staging Plan for MVP 2

#### Sub-stage 2.1: Identify and Annotate All Inbound Entry Points

**Objective**: Ensure that every method that initiates a new business journey within the `book-transfers` service is correctly annotated with `@BeginJourney`, establishing the `JourneyContext` at the earliest possible boundary.

**Actions**:
1.  **Scan for Entry Points**: Identify all `@RestController` methods, `@JmsListener` methods, and any other methods (e.g., in `CommandHandlers` if they are entry points for internal commands not originating from an external adapter) that initiate a new business flow.
2.  **Annotate with `@BeginJourney`**: For each identified entry point, add the `@BeginJourney` annotation. The `value` attribute will contain a SpEL expression to extract the appropriate `journeyName` from the method's arguments (e.g., `"#command.getJourneyName()"` or a hardcoded string like `"'PAYMENT_SUBMIT_V1'"`).
3.  **Verify `CommandBus` Coverage**: Ensure that all `CommandBus.send()` invocations are downstream of an `@BeginJourney` annotated method. If a `CommandBus.send()` is an entry point itself (e.g., from a scheduled task), it must also be covered.

#### Sub-stage 2.2: Refactor `ConfigurationPort` Direct Consumers

**Objective**: Eliminate direct dependencies on `ConfigurationPort` from all application-level components (e.g., `CommandHandlers`, `UseCases`), ensuring that only the `BeginJourneyAspect` uses it for initial context setup.

**Actions**:
1.  **Identify Direct Injections**: Find all classes that directly inject `ConfigurationPort` (excluding `BeginJourneyAspect`).
2.  **Replace with `JourneyContextManager`**: For each identified usage, refactor the code to obtain the `JourneySpecification` via `JourneyContextManager.getContext().specification()`.
3.  **Remove `ConfigurationPort` Injection**: Remove the `ConfigurationPort` field and its injection from these classes.

#### Sub-stage 2.3: Refactor `CommandProcessingContextHolder` Consumers

**Objective**: Eliminate all direct dependencies on the legacy `CommandProcessingContextHolder`, ensuring all components retrieve `JourneySpecification` from the new `JourneyContextManager`.

**Actions**:
1.  **Identify Usages**: Find all classes that call `CommandProcessingContextHolder.getContext()`.
2.  **Replace with `JourneyContextManager`**: For each identified usage, refactor the code to obtain the `JourneySpecification` via `JourneyContextManager.getContext().specification()`.
3.  **Remove Legacy Imports/Fields**: Remove `CommandProcessingContextHolder` imports and any associated fields.
4.  **Remove `CommandProcessingContext`**: Once `CommandProcessingContextHolder` is fully decommissioned, the `CommandProcessingContext` record will also be removed.

#### Sub-stage 2.4: Refactor `BusinessPolicyFactory` Usage

**Objective**: Ensure that all instances where `BusinessPolicy` is created or rehydrated consistently obtain the `JourneySpecification` from the `JourneyContextManager`.

**Actions**:
1.  **Scan for `policyFactory.create()`**: Identify all calls to `policyFactory.create(spec)` or `policyFactory.create(spec, payment)`.
2.  **Ensure Context-Derived `spec`**: Verify that the `spec` argument passed to these factory methods is always derived from `JourneyContextManager.getContext().specification()`. This might involve modifying method signatures if `spec` is currently passed as a parameter, or ensuring the caller retrieves it from the manager.
3.  **Review `Payment.startNew()` and `Payment.rehydrate()`**: Confirm that these methods (or their immediate callers) correctly obtain the `BusinessPolicy` using a `JourneySpecification` from the `JourneyContextManager`.

#### Sub-stage 2.5: Refactor Event Listeners for Context

**Objective**: Ensure that event-driven flows correctly establish or propagate the `JourneyContext` when they initiate a new logical journey.

**Actions**:
1.  **Identify Event-Driven Entry Points**: Scan for methods annotated with `@EventListener`, `@TransactionalEventListener`, or similar annotations that act as entry points for new logical operations.
2.  **Annotate with `@BeginJourney`**: For each event listener that represents the start of a new journey, add the `@BeginJourney` annotation. The SpEL expression will need to extract the `journeyName` from the event object itself (e.g., `"#event.getJourneyIdentifier()"`).
3.  **Specific Case: `PaymentRequiresComplianceCheck`**: Ensure the listener for this event is covered by `@BeginJourney` and correctly extracts the journey identifier from the `PaymentRequiresComplianceCheck` event object.

#### Sub-stage 2.6: Decommission Legacy Components

**Objective**: Remove all obsolete legacy context management components from the codebase.

**Actions**:
1.  **Delete `ConfigurationEnrichmentMiddleware.java`**: Once all its consumers are migrated (as per Sub-stage 2.3), this file can be safely deleted.
2.  **Delete `CommandProcessingContextHolder.java`**: Once all its consumers are migrated (as per Sub-stage 2.3), this file can be safely deleted.
3.  **Delete `CommandProcessingContext.java`**: This record is only used by `CommandProcessingContextHolder` and can be deleted once the holder is gone.
4.  **Review `ConfigurationPort`**: Confirm that `ConfigurationPort` is now *only* used by `BeginJourneyAspect`. If so, its purpose is now solely for the aspect's internal mechanism, and it remains a valid core port.

### Missing Anything for Seamless Coverage?

*   **Query Use Cases**: Any query use case that requires `JourneySpecification` (e.g., for policy-driven data filtering) can now simply call `JourneyContextManager.getContext().specification()`. If a query is an entry point, it should also be annotated with `@BeginJourney`. We will keep an eye out for such cases during Sub-stage 2.1.
*   **`Command.getIdentifier()`**: If any downstream component still needs this specific identifier *and it's not part of the `JourneySpecification`*, we'll need to ensure it's either passed explicitly or made part of the `JourneyContext` itself. However, the `JourneySpecification` should contain all necessary configuration.
*   **Logging**: Ensure that any logging previously done by the middleware (e.g., "Service config loaded") is either no longer necessary or is handled by the aspect or the `JourneyContextManager` if appropriate.

---
## 4. Design Decisions Log (Audit Trail)

<details>
<summary>Click to view the decision log</summary>

### 2023-10-27: AOP Framework Selection
**Decision**: We will use **Spring AOP**.
**Reasoning**: Sufficient for intercepting Spring-managed beans at the adapter layer and avoids the build complexity of AspectJ.

### 2023-10-27: Core Context Primitive Selection
**Decision**: We will use `java.lang.ScopedValue`.
**Reasoning**: A future-proof choice aligned with modern Java (Project Loom), replacing the legacy `ThreadLocal`. Requires enabling JDK preview features.

### 2023-10-27: Build Tooling Configuration
**Decision**: The Maven build will be configured for both compilation (`maven-compiler-plugin`) and testing (`maven-surefire-plugin`, `maven-failsafe-plugin`) to support `--enable-preview`.
**Reasoning**: Although writing tests is not the immediate focus, a complete and correct build configuration is a matter of architectural integrity. Omitting test plugin configuration would create a brittle build that fails on standard Maven lifecycle phases (e.g., `mvn install`). This preventative measure ensures the build is robust and stable, avoiding future failures at a low, one-time setup cost.

### 2023-10-27: Journey Identifier Extraction Strategy
**Decision**: The `@BeginJourney` annotation will use a **Spring Expression Language (SpEL)** string.
**Reasoning**: Provides maximum flexibility to non-intrusively adapt to any entry point method signature.

### 2023-10-27: Module Placement
**Decision**: Framework contracts in `core`, engine in `infrastructure`.
**Reasoning**: Strictly adheres to Hexagonal Architecture, keeping the core pristine and isolating technology-specific implementations.

### 2023-10-27: `JourneyContextHolder` Renaming and API Design
**Decision**: `JourneyContextHolder` will be renamed to `JourneyContextManager`. It will provide a single `runWithContext(JourneyContext context, Callable<T> operation) throws Exception` method. The `BeginJourneyAspect` will handle the `Throwable` wrapping/unwrapping to maintain transparency.
**Reasoning**: The new name accurately reflects the class's active role in managing context lifecycle. Providing a `Callable`-based API makes it clean and usable for application-level code, while the aspect's self-reliant `Throwable` handling ensures its transparency and architectural purity.

### 2023-10-27: Spring AOP Activation
**Decision**: Spring AOP will be activated by placing `@EnableAspectJAutoProxy` on `AspectFacadeConfiguration.java` within the `infrastructure` module.
**Reasoning**: This aligns with the project's existing facade pattern for infrastructure components, creating an atomic on/off switch for the AOP subsystem and reducing coupling with the `app` module.

</details>

---
<br/>

***OBSOLETE: The following plan was created prior to the formal decision-making process and is preserved for audit purposes.***

<details>
<summary>Click to view obsolete plan</summary>

... (previous obsolete content remains here) ...

</details>
