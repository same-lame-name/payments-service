# Pinnacle Design: The Journey Specification Lifecycle (Revived)

This document specifies the definitive, architecturally pure design for the propagation, validation, and access of the `JourneySpecification`. It supersedes all previous design documents and rectifies the critical security and design flaws identified in prior models.

---

### 1. The Flaw in Previous Designs: The Illusion of Control

Previous design iterations (including the "Gated Access" and "Scoped Accessor" models) were fundamentally flawed. They attempted to control access to the `JourneySpecification` via a method-level annotation (`@InJourney`) that guarded a `public static` accessor (`JourneyContext.spec()`).

This created a "docile bodyguard"—a security mechanism that could be trivially bypassed. A developer could simply call the static accessor from any method, completely ignoring the `@InJourney` annotation and its validation guarantees. This violates the core principle of a secure system: the safe path must be the *only* path.

**The core mistake was providing a discoverable, static API for access. This has been corrected.**

---

### 2. The Pinnacle Design: The Argument Injection Model

The new design is founded on a simple, unbreakable principle: **You cannot call an API that you cannot see.**

We will eliminate all static accessors for the journey context. The `JourneySpecification` will no longer be "retrieved" by developer code. Instead, it will be **injected** directly by the framework as a method parameter. This makes the dependency explicit, visible, and guaranteed by the method's own signature.

--- 

### 3. The Core Components

#### 3.1. The `@InJourney` Annotation (Redefined)

The annotation's role has been fundamentally changed. It no longer marks a method; it marks the parameter that is to receive the injected `JourneySpecification`.

*   **Target:** `ElementType.PARAMETER`
*   **Purpose:** To signal to the framework that a specific method parameter should be populated with the current, validated `JourneySpecification`.
*   **Signature:**
    ```java
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface InJourney {
        /**
         * Specifies a required contract interface that the JourneySpecification must conform to.
         */
        Class<?> requires() default JourneySpecification.class;
    }
    ```

#### 3.2. The `JourneyContext` Utility (Sealed)

The `JourneyContext` class now has **zero public API**. It is a `final` class with `package-private` methods, making it a hidden utility, completely invisible to application developers. Its sole purpose is to act as a shared secret between the framework's injection points (the middleware) and the parameter injection mechanism (the aspect).

*   **Visibility:** `package-private`
*   **Purpose:** To bind the `JourneySpecification` to a `ScopedValue` at the start of a request and provide a hidden accessor for the aspect to retrieve it.

#### 3.3. The `InJourneyParameterAspect`

This is the engine of the new design. It is an `@Around` aspect that intercepts the execution of any method that has a parameter annotated with `@InJourney`.

*   **Logic:**
    1.  **Intercept:** The aspect triggers when a method with an `@InJourney` parameter is called.
    2.  **Retrieve:** It uses the hidden, framework-internal `JourneyContext.getSpec()` method to retrieve the current `JourneySpecification` from the `ScopedValue`. If no spec is bound, it throws an `IllegalStateException`.
    3.  **Validate:** It inspects the `requires` attribute of the annotation and validates that the retrieved spec conforms to the required contract. If not, it throws a `ContractViolationException`.
    4.  **Inject:** It creates a new array of arguments for the target method, replacing the placeholder for the annotated parameter with the actual, validated `JourneySpecification` object.
    5.  **Proceed:** It invokes the target method with the modified argument list.

--- 

### 4. The Developer Experience: The Safe Lane is the Only Lane

This design makes the correct way of accessing the specification the only way. The developer's code becomes cleaner, more declarative, and inherently safer.

**Before (Flawed Design):**
```java
// A developer could bypass the annotation and its guards.
public void myFlawedMethod() {
    JourneySpecification spec = JourneyContext.spec(); // Unsafe, bypasses validation
    // ...
}
```

**After (Pinnacle Design):**
```java
// The dependency is explicit in the method signature. There is no other way.
public void myCorrectMethod(@InJourney JourneySpecification spec) {
    // The 'spec' variable is GUARANTEED by the framework to be non-null
    // and validated against any required contract before this code is ever reached.
    String journeyName = spec.getJourneyName();
}
```

### 5. Why This Design Is Superior and Non-Bypassable

*   **Zero Public API for Access:** There are no static methods for a developer to call. The concept of "getting" the spec is eliminated in favor of "receiving" it.
*   **Compile-Time Clarity:** The dependency is now part of the method's signature, making it explicit and visible to all callers and static analysis tools.
*   **Unbreakable Contract:** The aspect guarantees the parameter is present and valid *before* the developer's code is executed. There is no possibility of a `null` or incorrect type.
*   **Ultimate Simplicity:** The "fast lane" and the "safe lane" are now the same. The developer is guided to do the right thing by the very structure of the API.

This is the definitive design. It is not a suggestion; it is a guarantee.