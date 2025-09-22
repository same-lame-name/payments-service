# The Grand Design: A Generic Journey's Lifecycle

This document provides the canonical, high-level overview of a transaction's lifecycle within the system. It consolidates the architectural principles from all design documents into a single, sequential flow, representing the target state of our architecture.

The core principle is **"configuration over code,"** where the application's behavior is dynamically dictated by a `JourneySpecification` artifact, not hardcoded in business logic. The architecture is built on a series of non-negotiable guarantees that ensure safety and consistency.

---

### Phase 0: Startup-Time Integrity Guarantee

The application's lifecycle begins before it even accepts a request.

1.  **Action:** The Spring context initializes.
2.  **Mechanism:** A dedicated `JourneyIntegrityValidator` component executes.
3.  **Guarantee:** This validator scans every `JourneySpecification` defined in `application.yml` and cross-references it against the application's codebase. It verifies:
    *   **Component Existence:** Every bean name referenced in the YAML (for adapters, data collectors, business rules) corresponds to an actual Spring bean.
    *   **Validation Group Existence:** Every string in a `validation-groups` list corresponds to a real `ValidationGroups` interface.
    *   **Contract Conformance:** The journey's configuration satisfies its declared `JourneyContract`.
4.  **Outcome:** If any inconsistency is found (e.g., a typo in a bean name), an exception is thrown, and **the application will not start**. This eliminates an entire class of runtime errors by design.

---

### Phase 1: Request Entry & Syntactic Validation

This phase handles the initial acceptance and sanitization of an incoming request.

1.  **Entry & Anti-Corruption:** An HTTP request hits a `Controller`. The controller's sole responsibility is to act as an **Anti-Corruption Layer (ACL)**. It immediately maps the external `ApiRequest` DTO into a clean, internal `Command` object. This isolates the domain from the volatile API contract.

2.  **Scope Establishment:** The `JourneySpecification` is loaded based on the request context (e.g., headers, path). The `JourneyScopeAware` pattern guarantees that this specification is bound to a `ScopedValue`, making it safely available throughout the request's lifecycle without "magic" or `ThreadLocal`.

3.  **Context-Aware Syntactic Validation:** The `Command` is intercepted by a `CommandValidationMiddleware`.
    *   **Mechanism:** The middleware reads the `validation-groups` list from the `JourneySpecification`.
    *   **Guarantee:** It invokes the standard JSR 303 validator, instructing it to enforce *only* the validation rules on the `Command` object that are annotated with the active groups.
    *   **Outcome:** Malformed requests that violate the journey-specific API contract are rejected with a `400 Bad Request` before they can enter the business domain.

---

### Phase 2: Command Handling & Business Logic Orchestration

A syntactically valid `Command`, now within a guaranteed `JourneyScope`, is dispatched to its `CommandHandler`.

1.  **Runtime Contract Enforcement:** An AOP aspect (`ContractValidationAspect`) intercepts the call to the `CommandHandler.handle()` method.
    *   **Guarantee:** It inspects the handler's `@RequiresContract` annotation and verifies that the current `JourneySpecification` provides the necessary configuration (e.g., required port adapter routings). If the contract is not met, execution is halted, preventing a guaranteed runtime failure.

2.  **Data Collection (Enrichment):** The handler invokes a `DataCollectorOrchestrator`.
    *   **Mechanism:** The orchestrator reads a list of data collector bean names from the `JourneySpecification` (e.g., `dataCollectors: ["customerProfileCollector", "accountBalanceCollector"]`).
    *   **Execution:** It uses a `DataCollectorRegistry` to look up these beans and executes them **in parallel** (`CompletableFuture.allOf`) to enrich the context with all necessary external data.

3.  **Business Rule Validation (Semantic):** The handler invokes a `BusinessRuleOrchestrator`.
    *   **Mechanism:** The orchestrator reads a list of business rule bean names from the `JourneySpecification`.
    *   **Execution:** It uses a `BusinessRuleRegistry` to look up and execute these rules sequentially against the `Command` and the enriched data. A single rule failure halts the process.

---

### Phase 2.5: Semantic Validation with the Business Rule Engine

After enrichment, the `Command` is passed to the `BusinessRuleOrchestrator` for deep semantic validation. This engine is built on a declarative **Given-When-Then** philosophy.

1.  **Philosophy & Contract:**
    *   **Given:** The fully enriched `Command` object serves as the complete set of "facts" for all rules.
    *   **When:** Each rule must contain a guard method (annotated with `@When`) that checks for data presence and state applicability. This is a non-negotiable part of the rule contract.
    *   **Then:** The core validation logic (annotated with `@Then`) which is guaranteed to execute *only if* the `@When` guard passes.

2.  **Execution:**
    *   The orchestrator reads the list of active rule bean names from the `JourneySpecification`.
    *   It retrieves each rule from a `BusinessRuleRegistry` and executes them sequentially, respecting the `When -> Then` flow for each.
    *   **Guarantee:** The first rule to return an `Invalid` result **immediately halts** the entire validation chain, ensuring a fail-fast process.

### Phase 3: Transaction Execution

At this point, the request is fully validated and enriched. The developer now chooses how to execute the core transaction logic.

1.  **Orchestration (The "How"):**
    *   **Simple Workflow:** For linear processes, the `CommandHandler` can directly invoke the required outbound ports.
    *   **Complex Workflow:** For stateful or multi-step processes, the handler delegates to a state machine. It uses a `StateMachineFactoryDispatcher`, passing it the `JourneySpecification`. The dispatcher reads a key (e.g., `orchestration.engine`) to select and return the correct `StateMachineFactory` from its registry. The state machine then orchestrates the subsequent steps.

2.  **Port Dispatch (The "What"):**
    *   **Mechanism:** All interactions with external systems occur through a `Port.Dispatcher` interface (e.g., `DepositPort.Dispatcher.submitDeposit(...)`).
    *   **Guarantee:** The `PortDispatcher` implementation is a generic component that reads the adapter routing key from the `JourneySpecification` (e.g., `adapterRouting.depositPort: "DEPOSIT_PORT_REST"`). It uses this key to select the specific adapter bean (e.g., the REST adapter vs. the JMS adapter) from its internal map and routes the call.

This final step ensures the core domain remains pristine and ignorant of which external system is being called, fulfilling the primary goal of Hexagonal Architecture. The entire journey, from entry to exit, is governed by explicit, verifiable configuration.