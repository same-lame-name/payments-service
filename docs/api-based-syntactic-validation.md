### Design: API Syntactic Validation Framework

#### 1. The "Why": The Need for Context-Aware API Contract Validation

Our system exposes multiple, specific API endpoints (e.g., `/wallet/topup`, `/payment/schedule`), each with its own `Command` DTO. The validity of the data within these DTOs is not absolute; it is context-dependent, determined by the specific journey being executed. For example, a field might be mandatory for one journey but optional for another, even when using the same endpoint.

We require a robust, scalable, and declarative mechanism to enforce these context-aware API usage rules at the earliest possible moment, preventing invalid requests from ever entering our business domain.

#### 2. The "What": A Declarative, Two-Phase Validation Strategy

Our solution is a two-phase validation strategy that cleanly separates API contract validation from business rule validation.

*   **Phase 1: Syntactic & API Contract Validation:** This phase answers the question: **"Did the caller use our API correctly according to the journey's rules?"** It validates the *shape*, *format*, and *presence* of data in the incoming `Command` DTO. This is the focus of this document.

*   **Phase 2: Semantic & Business Rule Validation:** This phase occurs later, inside the `CommandHandler` (after data collection). It answers the question: **"Does this validly-formed request represent a valid business operation?"** (e.g., "Is the customer's balance sufficient?").

We will implement Phase 1 using the standard **Java Bean Validation (JSR 303)** framework, leveraging **Validation Groups** to manage context-dependency.

#### 3. The "When": In Middleware, Before Business Logic

Syntactic validation will be performed within a dedicated `CommandValidationMiddleware`. This middleware intercepts the `Command` immediately after it is deserialized in the controller and before it is dispatched to the `CommandBus`. This ensures that we fail fast, rejecting malformed requests before any expensive operations (data collection, handler logic) are attempted.

#### 4. The "How": A Framework of Definitions, Configuration, and Enforcement

The implementation is based on a clean separation of concerns: defining *what* a rule is in code, and deciding *when* it is active in configuration.

##### Step A: Defining the Rules in Code (The "What")

1.  **Validation Groups:** We will create a simple `ValidationGroups` interface to act as a type-safe namespace for our rule labels (e.g., `interface WalletJourney {}`, `interface ScheduledJourney {}`).

2.  **Annotating `Command` DTOs:** On our endpoint-specific `Command` DTOs, we will use standard JSR 303 annotations (`@NotNull`, `@Size`, `@Pattern`). Crucially, we will assign them to one or more validation groups (e.g., `@NotNull(groups = WalletJourney.class)`). These annotations are **dormant** by default.

3.  **Custom Cross-Field Constraints:** For complex rules (e.g., "either `fieldA` or `fieldB` must be present"), we will create custom, class-level JSR 303 constraint annotations (e.g., `@EitherOr(field1="payeeId", field2="payeeDetails", groups=... )`). This allows us to encapsulate complex validation logic into reusable, declarative annotations that fully integrate with the validation group system.

##### Step B: Activating the Rules in Configuration (The "When")

*   **`JourneySpecification`:** The `application.yml` file will be the single source of truth for activating rules. Each journey's `JourneySpecification` will contain a `validation-groups` list of strings (e.g., `validation-groups: ["WalletJourney", "ScheduledJourney"]`). This list declaratively states which validation rules to enforce for that specific journey.

##### Step C: Enforcing the Rules at Runtime (The "Enforcer")

1.  **`CommandValidationMiddleware`:** This middleware component is the engine. At runtime, it retrieves the `Command` object and the current `JourneySpecification`.

2.  **Group Resolution:** It resolves the list of string-based group names from the YAML into a `Set` of `Class<?>` objects (e.g., `"WalletJourney"` -> `ValidationGroups.WalletJourney.class`).

3.  **Validation Execution:** It invokes the standard Spring `Validator`, passing it the `Command` object and the resolved set of active validation groups. The validator then enforces only the rules on the `Command` that are labeled with one of the active groups. If any rule is violated, a `ConstraintViolationException` is thrown, and the request is rejected with a 400 Bad Request status.

##### Step D: Guaranteeing Configuration Integrity (The Fail-Fast Guard)

To prevent silent failures from typos in the YAML configuration, the `JourneyIntegrityValidator` will be enhanced. At application startup, it will:

1.  Scan the `ValidationGroups` interface to build a master set of all valid group names.
2.  For every journey defined in `application.yml`, it will verify that every string in the `validation-groups` list corresponds to a known, valid group name from the master set.
3.  If an unknown group name is found, it will throw an exception and **halt application startup**, ensuring configuration errors are caught immediately.
