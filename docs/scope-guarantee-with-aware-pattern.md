### Design: The `JourneyScopeAware` Pattern for Guaranteed Scope

This document details the final architectural design for guaranteeing the presence of the `JourneySpecification` `ScopedValue` for any code that requires it. This pattern is designed to be an ironclad, multi-layered guarantee that eliminates the risk of `NoSuchElementException` at runtime.

--- 

### 1. The "Why": The Problem of Assumed Context

Our architecture is critically dependent on the `JourneySpecification` being available in a `ScopedValue` for any journey-aware business logic. Relying on developer discipline to manually wrap all possible entry points (controllers, listeners, schedulers) in a `ScopedValue.where(...).call(...)` block is a policy of hope, not a strategy. This "hopeful coverage" is fragile and destined to fail, leading to `NoSuchElementException`s in production when an untested code path is executed without the proper scope being established.

We require a system that provides a **verifiable guarantee** that any code declaring a dependency on the journey scope is, in fact, executed within that scope.

--- 

### 2. The "What": A Cohesive, Contract-Driven Framework

Our solution is a unified framework called the "`JourneyScopeAware` Pattern." It consists of three core components that work together to provide guarantees at compile-time, design-time, and runtime. The core principle is to make the dependency on the journey scope **explicit, verifiable, and self-providing**.

1.  **The `JourneyScopeAware` Interface:** The exclusive gateway for accessing the journey scope.
2.  **The `@EstablishJourneyScope` Annotation:** A declarative contract for methods that act as new entry points into a scoped context.
3.  **The `JourneyScopeAwareAspect`:** A single, powerful aspect that acts as a "guardian and provider," enforcing the contract at runtime.

--- 

### 3. The "How": The Three Components in Detail

#### Component A: The `JourneyScopeAware` Interface (The Gateway)

*   **What:** A simple interface that any class needing access to the `JourneySpecification` must implement.

*   **Why:** This is the cornerstone of the **compile-time guarantee**. By controlling access to the `ScopedValueReader` through this interface, we prevent any arbitrary class from attempting to access the `ScopedValue`. It forces a developer to make a conscious, explicit decision to enter the "scoped world."

*   **How:** The interface provides a `default` method that returns a singleton instance of a `ScopedValueReader`. This is the only public way to obtain the reader. A developer cannot access `JourneySpecification.current()` directly; they must first `implement JourneyScopeAware` and then call `getJourneySpecReader().get()`.

    ```java
    public interface JourneyScopeAware {
        default ScopedValueReader<JourneySpecification> getJourneySpecReader() {
            return JourneySpecificationReader.INSTANCE;
        }
    }
    ```

#### Component B: The `@EstablishJourneyScope` Annotation (The Entry Point Contract)

*   **What:** A method-level annotation used *inside* a `JourneyScopeAware` class. It declares that a specific method is a new entry point and provides the recipe for creating the scope.

*   **Why:** This solves the problem of the aspect needing to "wishfully" find the `journeyIdentifier`. It makes the contract explicit. The developer annotating the method takes on the responsibility of telling the aspect exactly how to find the necessary identifier from the method's arguments.

*   **How:** The annotation contains a `value()` member that accepts a **Spring Expression Language (SpEL)** string. This provides a powerful and declarative way to specify the location of the `journeyIdentifier` without writing custom extractor code.

    ```java
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface EstablishJourneyScope {
        String value(); // e.g., "#event.journeyId" or "#journeyIdentifier"
    }
    ```

#### Component C: The `JourneyScopeAwareAspect` (The Guardian and Provider)

*   **What:** A single `@Around` aspect that targets any method execution within any class that implements `JourneyScopeAware`.

*   **Why:** This is the runtime enforcer—the "judge, jury, and executioner." It guarantees that the contract defined by the interface and annotation is always fulfilled. It is not a passive "whistleblower" that just throws an exception; it is an active guardian that provides the scope when necessary and possible.

*   **When:** It intercepts every method call on a `JourneyScopeAware` bean.

*   **How:** The aspect's logic is precise and ordered:
    1.  **Check for Existing Scope:** It first checks `JourneySpecification.isBound()`. If `true`, a scope already exists (e.g., this is a nested call within an already-established context). The aspect does nothing and immediately proceeds with the method execution. This is efficient and safe.
    2.  **Check for Entry Point Contract:** If no scope is bound, the aspect inspects the specific method being called. It checks if the method is annotated with `@EstablishJourneyScope`.
    3.  **Provide the Scope:** If the annotation is present, the aspect uses the SpEL expression from the annotation to extract the `journeyIdentifier` from the method's arguments. It then loads the `JourneySpecification` and wraps the original method's execution in a `ScopedValue.where(...).call(...)` block. The contract is fulfilled; the method executes safely within the newly created scope.
    4.  **Enforce the Guarantee:** If no scope is bound AND the method is **not** annotated with `@EstablishJourneyScope`, the aspect's final duty is to throw an `IllegalStateException`. This is a critical failure condition. It means a developer has called a non-entry-point method on a `JourneyScopeAware` bean from an invalid, non-scoped context. The exception message is precise, identifying the method and the nature of the architectural violation.

--- 

### 4. The Result: A Complete, Multi-Layered Guarantee

This design provides an ironclad guarantee through three layers:

1.  **Compile-Time:** No developer can even get access to the `ScopedValueReader` without implementing `JourneyScopeAware`.
2.  **Design-Time:** Developers are forced to think about which methods are entry points and explicitly annotate them with `@EstablishJourneyScope`, making the architecture self-documenting.
3.  **Runtime:** The `JourneyScopeAwareAspect` ensures that no method on a `JourneyScopeAware` bean can ever execute without a scope being present, either by using an existing one or creating one on-demand, as dictated by the contract.
