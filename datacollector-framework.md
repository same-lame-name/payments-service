# The Journey-Aware Command Framework: An Architectural Implementation Plan

**Version:** 2.1
**Status:** Final & Authoritative

---

## 1. Foreword: The Philosophy of API Transparency

This document details the definitive architecture for a command enrichment and configuration-aware framework. It is founded on a single, supreme principle: **The command object passed to a handler must be a complete, self-contained, and honest representation of all available context.**

We explicitly reject the use of "magical boxes" or parallel context carriers (e.g., `ScopedValue`, `ThreadLocal`) for carrying primary operational data like blueprints and enrichment fragments. Such mechanisms create hidden dependencies, forcing developers to look outside a method's signature to understand its capabilities. This is a source of fragility and cognitive overhead.

Instead, we embrace **API Transparency**. A method's signature is its most honest contract. By placing all necessary data directly on the command object, the `handle(MyCommand command)` signature becomes a complete and truthful declaration.

To achieve this, we accept a pragmatic trade-off: **Controlled Mutability**. We will allow the command object to be mutated, but only in a highly controlled, framework-managed manner. The core business intent of the command remains immutable, while framework-specific context is attached to it during a well-defined middleware phase. This is a conscious architectural choice that prioritizes clarity, discoverability, and explicit contracts over the dogma of absolute immutability.

---

## 2. The Grand Design: A Unified, Journey-Aware Command Hierarchy

The architecture is built upon a new hierarchy of command interfaces. `JourneyAwareCommand` will serve as the foundational contract for any command that participates in advanced framework behaviors. More specific behavioral interfaces, such as `IdempotentCommand` and `EnrichableCommand`, will extend this foundation.

This creates a highly cohesive system where middlewares can inspect a command, understand its capabilities via `instanceof` checks, and interact with it directly to get configuration (the blueprint) and attach new data (enrichment fragments).

The flow is as follows:
1.  A command that opts into this pattern will implement the `JourneyAwareCommand` interface (and others).
2.  The `ConfigurationEnrichmentMiddleware` will attach the journey-specific `Blueprint` to the command.
3.  Subsequent middlewares (for idempotency, enrichment, etc.) will get the blueprint *directly from the command object*, use it to make decisions, and, if necessary, mutate the command by attaching further data.
4.  The final command handler receives a single, fully hydrated command object that is the single source of truth for the entire operation.

---

## 3. Step 1: The `common-domain` Foundational Contracts

These minimal, generic contracts will reside in the `common-domain` library.

### 3.1: `Blueprint.java`
A top-level, generic marker interface. This is a critical architectural component that prevents a circular dependency between `common-domain` and service-specific modules.

```java
package dexter.banking.common.journey;

/**
 * A top-level marker interface for any journey blueprint configuration object.
 */
public interface Blueprint {
}
```

### 3.2: `JourneyAwareCommand.java`
The foundational interface, now with powerful, type-safe accessors for the blueprint.

```java
package dexter.banking.common.journey;

import dexter.banking.commandbus.Command;

/**
 * The foundational interface for any command that is aware of its journey blueprint.
 */
public interface JourneyAwareCommand<R> extends Command<R> {
    /**
     * Framework-facing method to attach the journey blueprint.
     */
    void setBlueprint(Blueprint blueprint);

    /**
     * Retrieves the blueprint and casts it to the requested type.
     * @throws ClassCastException if the blueprint is not of the requested type.
     */
    <T extends Blueprint> T getBlueprint(Class<T> blueprintType);

    /**
     * Retrieves the blueprint, inferring the type from the assignment context.
     * @throws ClassCastException if the blueprint cannot be cast to the inferred type.
     */
    <T extends Blueprint> T getBlueprint();
}
```

### 3.3: `IdempotentCommand.java` & `EnrichableCommand.java`
These behavioral interfaces extend the `JourneyAwareCommand` foundation.

```java
package dexter.banking.common.idempotency;

import dexter.banking.common.journey.JourneyAwareCommand;

public interface IdempotentCommand<R> extends JourneyAwareCommand<R> {
    String getIdempotencyKey();
}
```

```java
package dexter.banking.common.enrichment;

import dexter.banking.common.journey.JourneyAwareCommand;
import java.util.Map;
import java.util.Optional;

public interface EnrichableCommand<R> extends JourneyAwareCommand<R> {
    void enrich(Map<Class<? extends EnrichmentFragment>, EnrichmentFragment> fragments);
    <F extends EnrichmentFragment> Optional<F> get(Class<F> fragmentType);
}
```

### 3.4: `EnrichmentFragment.java`
The generic marker for data fragments.

```java
package dexter.banking.common.enrichment;

public interface EnrichmentFragment {
}
```

---

## 4. Step 2: The `book-transfers` Service Implementation

The `book-transfers` service will implement these contracts.

### 4.1: `JourneyBlueprint.java`
The service-specific blueprint interface extends the common `Blueprint`.

```java
package dexter.banking.booktransfers.core.domain.shared.blueprint;

import dexter.banking.common.journey.Blueprint;

public interface JourneyBlueprint extends Blueprint {
}
```

### 4.2: Concrete Command Implementation
A concrete command like `PaymentCommand` must be a `class` and will implement the desired combination of interfaces, including the new type-safe blueprint getters.

```java
package dexter.banking.booktransfers.core.application.payment.command;

import dexter.banking.common.journey.Blueprint;
// ... other imports

public class PaymentCommand implements IdempotentCommand<PaymentResult>, EnrichableCommand<PaymentResult> {

    // --- Core, Immutable Business Intent ---
    private final String transactionId;
    // ...

    // --- Mutable, Framework-Controlled State ---
    private Blueprint blueprint;
    private Map<Class<? extends EnrichmentFragment>, EnrichmentFragment> fragments;

    // ... constructor ...

    // --- Framework-Facing Setters ---
    @Override
    public void setBlueprint(Blueprint blueprint) { this.blueprint = blueprint; }

    @Override
    public void enrich(Map<Class<? extends EnrichmentFragment>, EnrichmentFragment> fragments) { /*...*/ }

    // --- Public Accessors for Handler ---
    @Override
    public <T extends Blueprint> T getBlueprint(Class<T> blueprintType) {
        if (this.blueprint == null) throw new IllegalStateException("Blueprint not set.");
        return blueprintType.cast(this.blueprint);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Blueprint> T getBlueprint() {
        if (this.blueprint == null) throw new IllegalStateException("Blueprint not set.");
        return (T) this.blueprint;
    }

    @Override
    public <F extends EnrichmentFragment> Optional<F> get(Class<F> fragmentType) { /*...*/ }

    // ... other required overrides ...
}
```

---

## 5. Step 3: The Cohesive Middleware Chain

The middlewares remain simple and self-reliant.

### 5.1: `ConfigurationEnrichmentMiddleware` (@Order(1))
Attaches the blueprint.

```java
// In invoke() method...
if (command instanceof JourneyAwareCommand) {
    // ... find spec from provider ...
    ((JourneyAwareCommand<?>) command).setBlueprint(spec.getBlueprint());
}
return next.invoke(command);
```

### 5.2: `IdempotencyMiddleware` (@Order(2))
Gets the blueprint directly from the command.

```java
// In invoke() method...
if (command instanceof IdempotentCommand) {
    var idempotentCmd = (IdempotentCommand<?>) command;
    // Use the type-safe getter for clarity, though direct access is possible
    var blueprint = idempotentCmd.getBlueprint(BaseJourneyBlueprint.class);

    if (blueprint != null && blueprint.isIdempotencyEnabled()) {
        // ... perform idempotency check ...
    }
}
return next.invoke(command);
```

### 5.3: `EnrichmentMiddleware` (@Order(3))
Gets the blueprint directly from the command.

```java
// In invoke() method...
if (command instanceof EnrichableCommand) {
    var enrichableCmd = (EnrichableCommand<?>) command;
    var blueprint = enrichableCmd.getBlueprint(BaseJourneyBlueprint.class);
    List<String> collectorNames = blueprint.getDataCollectors();

    if (collectorNames != null && !collectorNames.isEmpty()) {
        // ... perform parallel data collection ...
        enrichableCmd.enrich(fragments);
    }
}
return next.invoke(command);
```

---

## 6. Step 4: The Final, Supremely Clean Usage Pattern

The command handler is now as clean and transparent as possible, with no casting required for blueprint access.

```java
@Service
public class SubmitPaymentV1CommandHandler implements CommandHandler<PaymentCommand, PaymentResult> {

    @Override
    public PaymentResult handle(PaymentCommand command) {
        // No casting. Type is inferred from the variable declaration.
        StandardPaymentBlueprint blueprint = command.getBlueprint();

        // Accessing enrichment data
        CustomerProfile profile = command.get(CustomerProfileFragment.class)
            .map(CustomerProfileFragment::profile)
            .orElseThrow(() -> new IllegalStateException("Customer profile is required"));

        // ... proceed with business logic ...
    }
}
```
