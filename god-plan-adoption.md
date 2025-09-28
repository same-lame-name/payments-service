# The God-Plan: Application Adoption Strategy

**Version:** 1.0
**Status:** Active & Authoritative

---

## 1. Foreword: The Harvest

This document is the high-level strategic guide for refactoring the `book-transfers` application to adopt the **Configuration-over-Code Framework** (the implementation of which is detailed in `god-plan-framework.md`).

Its purpose is to guide the transformation of the application's core into a pristine, generic, and declarative engine. This is the "harvest" phase, where we reap the benefits of safety, flexibility, and maintainability promised by the framework.

---

## 2. The Core Transformation: From Doer to Delegator

### The "Why"

The existing application core (`Controllers`, `CommandHandlers`, `StateMachines`) is burdened with the architectural sins of the past: hardcoded dependencies, imperative `if/else` logic, and implicit assumptions. This makes the codebase brittle, difficult to understand, and resistant to change.

### The "What"

The goal is to transform our core components from **"doers"** into **"delegators."**

A `CommandHandler` should no longer contain complex, journey-specific logic. Instead, it should be a lean orchestrator that simply declares its needs to the framework and delegates all complex operations—enrichment, validation, and routing—to it. The framework, guided by the type-safe `JourneyBlueprint`, does the heavy lifting.

---

## 3. The Refactoring Blueprint (Executive Summary)

This section provides a high-level overview of the required code transformations.

### Sub-step 3.1: Refactor Use Cases (`CommandHandlers`)

This is the most critical transformation. Handlers will be stripped of all specific dependencies and imperative logic.

**Before (Rigid, Imperative Handler):**

```java
@Component
public class OldSubmitPaymentCommandHandler implements CommandHandler<SubmitPaymentCommand> {
    
    @Autowired @Qualifier("restDepositAdapter")
    private DepositPort restAdapter;

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

        // Hardcoded logic
        restAdapter.submitDeposit(...);
    }
}
```

**After (The God-Plan Way - Lean, Declarative Handler):**

```java
@Component
public class SubmitPaymentCommandHandler implements CommandHandler<SubmitPaymentCommand> {

    // Inject the FRAMEWORK orchestrators
    private final DataCollectorOrchestrator dataCollector;
    private final BusinessRuleOrchestrator ruleOrchestrator;

    public SubmitPaymentCommandHandler(/*...inject orchestrators...*/) { ... }

    @Override
    public void handle(
        SubmitPaymentCommand command,
        // Declare dependency on the specific, type-safe blueprint
        @InJourney StandardPaymentBlueprint blueprint
    ) {
        // 1. Delegate Enrichment to the framework
        EnrichedCommand enrichedCommand = dataCollector.enrich(command, blueprint);

        // 2. Delegate Validation to the framework
        ruleOrchestrator.execute(enrichedCommand, blueprint)
            .getOrElseThrow(violations -> new BusinessRuleValidationException(violations.toString()));

        // 3. Delegate Orchestration to the framework
        StateMachineFactory<SubmitPaymentCommand> factory = blueprint.orchestration().engine();
        StateMachine<States, Events> stateMachine = factory.create(enrichedCommand, blueprint);
        stateMachine.start();
    }
}
```

### Sub-step 3.2: Refactor Outbound Port Interactions (e.g., in `StateMachines`)

All direct dependencies on specific port adapters or dispatchers must be removed.

**Before (Tightly Coupled State Machine):**

```java
@WithStateMachine
public class StandardPaymentStateMachine {
    // Wrong: Depends on a specific implementation
    private final RestDepositAdapter depositAdapter;

    private void doSubmitDeposit(StateContext<S, E> context) {
        // This call is hardcoded to the REST adapter
        depositAdapter.submitDeposit(request);
    }
}
```

**After (Decoupled State Machine via Blueprint):**

```java
@WithStateMachine
public class StandardPaymentStateMachine {
    // Correct: No dependency on any specific adapter or dispatcher.

    private void doSubmitDeposit(
        StateContext<S, E> context,
        // The framework injects the blueprint automatically from the established context
        @InJourney StandardPaymentBlueprint blueprint
    ) {
        SubmitDepositRequest request = ...;

        // Get the correct adapter directly from the blueprint proxy.
        // This is a COMPILE-TIME SAFE method call!
        DepositPort adapter = blueprint.adapterRouting().depositPort();
        adapter.submitDeposit(request);
    }
}
```

### Sub-step 3.3: Purify Inbound Adapters (`Controllers`)

The controller's responsibility is reduced to that of a pure Anti-Corruption Layer (ACL).

**The Goal:** A controller method should do nothing more than:
1.  Map the incoming request DTO to an internal `Command` object.
2.  Send the command to the `CommandBus`.

All other concerns (context setup, syntactic validation) are now handled automatically by the framework's middleware (`JourneyContextMiddleware`, `CommandValidationMiddleware`).

---

## 4. The Pinnacle State

Upon completion of this refactoring, the `book-transfers` service will be a model of modern, configurable software design.

*   **Ultimate Flexibility:** Adding a new payment journey that reuses existing logic but calls a new adapter is now trivial. We simply implement the new adapter and add a new journey definition to `application.yml`. No changes to the core domain logic are required.
*   **Fearless Refactoring:** The core logic is now so simple, high-level, and declarative that it is trivial to read, understand, and maintain.
*   **Architectural Purity:** The Hexagonal Architecture is no longer just a theoretical diagram; it is a living, breathing reality enforced by the framework. The core is pristine and completely isolated from the messy details of the outside world.
