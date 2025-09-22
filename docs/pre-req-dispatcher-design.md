### Architectural Prerequisite: The Journey Contract & Validation Framework

#### 1. The "Why": The Problem of Unverifiable Configuration

A system driven by configuration is only as robust as its validation. Without a formal link between an implementation's needs and its configuration, we are exposed to significant risks:

*   **Runtime Failures:** Simple typographical errors or incomplete configuration in YAML files can cause transaction failures.
*   **Incorrect Dispatch:** A `CommandHandler` could be dispatched a journey it is not equipped to handle, leading to unpredictable errors.
*   **High Maintenance Overhead:** Debugging configuration-related issues becomes a process of manual inspection and guesswork.

To build a reliable dispatcher ecosystem, we must first establish a framework that guarantees the correctness, completeness, and appropriate usage of our `JourneySpecification` configuration.

#### 2. The "What": A Multi-Layered, End-to-End Validation Strategy

Our solution is an unbroken chain of guarantees that spans the entire software lifecycle: from compile-time to startup-time to runtime dispatch. This ensures that a misconfigured or incorrectly dispatched journey can never be executed.

#### 3. The "How": The Three Layers of Defense

##### Layer 1: Compile-Time Guarantee (Structural Integrity)

We will use the Java compiler to enforce the structural integrity of our contracts and their usage.

*   **The Type-Safe `JourneyContract`:** We will introduce a `JourneyContract` interface. This contract is **pure**: it is only concerned with defining the **ports** a `CommandHandler` will directly invoke. Concrete implementations will use **Method References** (e.g., `AdapterRouting::depositPort`) to declare their requirements. This creates a compile-time link between the contract and the `JourneySpecification`’s structure.
*   **The `@RequiresContract` Annotation:** A new `@RequiresContract` annotation will take a `.class` literal (e.g., `@RequiresContract(StandardPaymentContract.class)`), ensuring that the contract being referenced actually exists at compile time.

##### Layer 2: Startup-Time Guarantee (Configuration Correctness)

We will validate the entire set of journey configurations when the application boots. This is the responsibility of the `JourneyIntegrityValidator`.

*   **The `JourneyIntegrityValidator`:** This component runs once at startup and performs a comprehensive, two-part validation on every `JourneySpecification` defined in the configuration:
    1.  **Part 1: Contract Conformance Validation:** The validator first checks the requirements of the `CommandHandler`. It uses the journey’s declared `JourneyContract` to verify that the YAML configuration is *complete* with respect to the handler's needs. It ensures all port adapter fields required by the contract are present and non-empty.
    2.  **Part 2: Global Component Existence Validation:** After verifying contract conformance, the validator performs a global check on the *entire* `JourneySpecification`. It iterates through **all** fields that are configured as Spring bean references—including `orchestrationEngine`, all `adapterRouting` values, and all entries in the `dataCollectors` and `businessRules` lists. For each reference, it confirms that a bean with that exact name exists in the corresponding component registry.

Any failure in this multi-part validation throws an exception and halts application startup. This guarantees that no journey can be configured with missing handler dependencies OR with references to non-existent components.

##### Layer 3: Runtime-Dispatch Guarantee (Execution Precondition)

The final guarantee is enforced just before execution, ensuring a method only runs if its contractual preconditions are met. This is achieved non-invasively using **Aspect-Oriented Programming (AOP)**.

*   **The `@RequiresContract` Annotation:** This annotation can be placed on any method, most notably `CommandHandler.handle`, to declaratively state its required `JourneyContract`.
*   **The `ContractValidationAspect`:** A "before" advice aspect targets any method decorated with `@RequiresContract`. Before the target method is executed, the aspect:
    1.  Retrieves the required `JourneyContract` class from the annotation.
    2.  Retrieves the current `JourneySpecification` from its `ScopedValue`.
    3.  Compares the required contract with the one specified in the journey specification using `isAssignableFrom` to support contract inheritance.
    4.  If they do not match, or if no specification is found, the aspect throws a `ContractViolationException`, preventing the method from executing.

This framework is the mandatory prerequisite for the dispatcher design. It ensures that the configuration the dispatchers will rely on is guaranteed to be valid, complete, and correct for the given context.
