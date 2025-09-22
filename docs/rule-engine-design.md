# Design: The Declarative Business Rule Engine

This document details the architecture for the semantic business rule validation engine. It is designed to execute complex, state-dependent business logic after a `Command` has been syntactically validated and fully enriched with all necessary external data.

---

### 1. The "Why": The Need for Semantic Validation

While the `api-based-syntactic-validation` framework answers the question, *"Did the caller use our API correctly?"*, it does not and cannot answer the more complex question: ***"Does this validly-formed request represent a valid business operation?"***

This second category of validation, known as semantic validation, requires a complete data context. For example, one cannot check for sufficient funds without first fetching the customer's account balance. These rules are numerous, often interdependent, and specific to the business journey.

A simple `if/else` block within a `CommandHandler` is not a scalable or maintainable solution. It leads to tangled, monolithic logic that is difficult to test, reuse, and configure. We require a dedicated framework that allows rules to be defined as independent, reusable, and configurable components.

---

### 2. The "What": A Declarative, `Given-When-Then` Framework

Our solution is a home-grown, annotation-driven rule engine inspired by the **`Given-When-Then`** philosophy of Behavior-Driven Development (BDD). It provides a clear, declarative structure for defining and executing business rules.

The core components are:

*   **`@BusinessRule` Annotation:** A stereotype annotation that marks a POJO as a self-contained, discoverable business rule component.
*   **`@When` Annotation:** Marks a "guard" method within a rule. This method evaluates the rule's precondition.
*   **`@Then` Annotation:** Marks the "action" method containing the core validation logic.
*   **`BusinessRuleRegistry`:** A Spring-managed component that discovers all `@BusinessRule` beans at startup and prepares them for execution.
*   **`BusinessRuleOrchestrator`:** The engine that receives a `Command`, reads the list of active rules from the `JourneySpecification`, and executes them in sequence.

The philosophy is implemented as follows:
*   **Given:** The fully enriched `Command` object serves as the implicit set of "facts" for the rule.
*   **When:** The `@When` annotated method acts as a data-presence and state-applicability guard. It must return `true` for the rule's logic to be considered.
*   **Then:** The `@Then` annotated method contains the validation logic and is **only** executed if the `@When` guard passes.

---

### 3. The "When": After Data Enrichment, Before Transaction Execution

The `BusinessRuleOrchestrator` is invoked from within the `CommandHandler`, after all `DataCollector`s have completed their work and the `Command` object is fully enriched.

This placement is critical. It ensures that by the time the rules are executed, they have a complete and consistent view of the state required to make a valid business decision.

If any rule fails, the orchestrator returns an `Invalid` result. The `CommandHandler` is responsible for catching this failure, throwing a `BusinessRuleValidationException`, and halting any further processing. This ensures a fail-fast mechanism that prevents invalid operations from proceeding.

---

### 4. The "How": A Spring-Native, Annotation-Driven Implementation

The engine is designed for seamless integration with our existing architecture.

1.  **Rule Definition (The Developer Experience):**
    *   A developer creates a new class and annotates it with `@BusinessRule("myUniqueRuleName")`.
    *   They create a public method annotated with `@When` that accepts the `Command` and returns a `boolean`. This method contains the guard logic (e.g., `return command.getAccountDetails() != null;`).
    *   They create a second public method annotated with `@Then` that accepts the `Command` and returns a `Validation<List<String>, Command>`. This method contains the core business logic.

2.  **Configuration (The Journey Architect Experience):**
    *   The architect adds the rule's bean name (`"myUniqueRuleName"`) to the `business-rules` list within the appropriate `JourneySpecification` in `application.yml`.

3.  **Execution (The Runtime Mechanics):**
    *   At startup, the `BusinessRuleRegistry` scans the `ApplicationContext` for all `@BusinessRule` beans and caches them in a map. For performance, it also uses reflection once to find and cache the `@When` and `@Then` methods for each rule.
    *   When a request is processed, the `CommandHandler` calls `businessRuleOrchestrator.execute(command, journeySpec)`.
    *   The orchestrator gets the list of rule names from the `journeySpec`.
    *   For each name, it retrieves the rule bean and its cached executor methods from the registry.
    *   It invokes the `@When` method. If it returns `false`, the rule is skipped.
    *   If it returns `true`, it invokes the `@Then` method.
    *   If the `@Then` method returns an `Invalid` `Validation` object, the orchestrator immediately stops and returns this invalid result to the `CommandHandler`, short-circuiting the entire process.

This design provides a robust, scalable, and maintainable framework that perfectly aligns with our "configuration over code" principle. It empowers developers to create decoupled, testable business rules, and allows architects to compose them into complex validation chains with simple YAML configuration.