### Architectural Design: The Configurable Dispatcher Ecosystem

**Prerequisite:** This design assumes the successful implementation of the validation framework specified in `pre-req-dispatcher-design.md`.

#### 1. The "Why": The Problem of Rigidity

The current architecture suffers from a critical flaw: rigidity. Orchestration logic and component selection are hardcoded within command handlers and state machines, most evident in the use of multiple `@Qualifier` annotations to inject specific `StateMachineFactory` beans. This approach is unscalable and prevents the rapid configuration of new journeys.

Our primary directive is to evolve the system from its current state of "code over configuration" to "configuration over code."

#### 2. The "What": The Solution - A Dispatcher & Registry Ecosystem

We will refactor the architecture to be dynamically driven by the `JourneySpecification`. The core principle is to externalize all journey-specific decisions into this configuration artifact, leaving the application's core code pristine, generic, and reusable.

We will achieve this by introducing a consistent "Dispatcher/Registry" pattern for all points of variation in the command processing flow. This includes:

1.  **State Machine Selection:** Choosing the correct orchestration engine (`StateMachine`).
2.  **Data Collection:** Executing pre-processing steps to gather required data from external systems.
3.  **Business Rule Validation:** Running journey-specific validation logic.
4.  **Transaction Leg Execution:** Interacting with outbound ports (e.g., for deposits, credits).

The existing `CommandHandler` -> `StateMachine` flow will be preserved, but the state machine itself will become a generic orchestrator that consults the `JourneySpecification` at each step.

#### 3. The "How": The Implementation Strategy

We will implement this vision through a series of precise architectural components that consume the validated `JourneySpecification`:

*   **The `StateMachineFactoryDispatcher`:** This will be the single point of entry for state machine creation. A command handler (or a middleware component) will no longer be injected with multiple factories. Instead, it will receive a single `StateMachineFactoryDispatcher` and pass the `JourneySpecification` to it. The dispatcher will read a configuration key (e.g., `journey.orchestration.engine`) and return the appropriate `StateMachineFactory` from an internal registry.

*   **The Generic Component Registry:** We will generalize the `PortDispatcher` pattern outlined in `next.md` into a universal mechanism for all pluggable components. For each component type (`DataCollector`, `BusinessRule`, etc.), we will create:
    *   A **Core Interface** defining the component's contract.
    *   An **Infrastructure Registry** (a Spring Bean) that collects all implementations of the interface into a `Map<String, ComponentInterface>`, keyed by their bean name.

*   **The Configurable State Machine:** The state machine's logic will be modified to:
    1.  **Delegate to Orchestrators:** At specific states (e.g., "DATA_COLLECTION"), the machine will invoke a dedicated orchestrator (e.g., `DataCollectorOrchestrator`).
    2.  **Read from `JourneySpecification`:** This orchestrator will read the list of required components from the `JourneySpecification` (e.g., `dataCollectors: ["A", "B"]`).
    3.  **Execute via Registry:** It will use the corresponding `Registry` to look up and execute the specified components. For data collection, this will be done in parallel using `CompletableFuture.allOf` to maximize performance.
    4.  **Invoke Port Dispatchers:** For transaction legs, the state machine will continue to call the port interface (e.g., `DepositPort.Dispatcher`), relying on the existing `PortDispatcher` implementation to route the call based on the `JourneySpecification`.
