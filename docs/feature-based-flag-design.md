# Design: A Type-Safe and Extensible Feature Flag Ecosystem

## 1. The Objective

This document specifies the complete architectural design for a feature flagging system. The primary goals of this system are to provide robust operational control (for maintenance, pilot programs, and progressive rollouts) while adhering to our established architectural principles: type-safety, loose coupling, and high performance.

## 2. Core Principles

The design is built upon the following non-negotiable principles:

*   **Type-Safety**: The system must prevent common errors by using strong types (enums) for configuration, eliminating "stringly-typed" code.
*   **Separation of Concerns**: Operational logic (feature flagging) is a cross-cutting concern and must be decoupled from core business logic (`CommandHandlers`).
*   **Dumb Adapters, Smart Core**: Infrastructure adapters (e.g., for caching) must be "dumb" actors that execute policy defined in the application's core, not contain their own intelligence.
*   **Performance by Design**: The system must be highly performant, avoiding unnecessary I/O in the request hot path through lazy loading and proactive caching.
*   **Extensibility**: The system must be extensible, allowing for new, complex authorization rules to be added without modifying the core framework.

## 3. The End-to-End Design

The ecosystem consists of domain models, application services, ports, and infrastructure adapters that work in concert to provide a seamless and performant feature flagging capability.

### 3.1. Core Domain Models

These are the foundational, immutable data structures.

*   **`UserGroup.java`**: A type-safe `enum` that provides a canonical list of all valid user groups.
    ```java
    public enum UserGroup {
        INTERNAL_AUDITORS,
        BETA_TESTERS_WAVE_1,
        APP_V2_USERS;
    }
    ```
*   **`User.java`**: An immutable record representing the user, using the `UserGroup` enum for its memberships.
    ```java
    public record User(String userId, Set<UserGroup> groups) {
        // Constructor for a partially hydrated user with no specific groups.
        public User(String userId) {
            this(userId, Set.of());
        }
    }
    ```
*   **`JourneySpecification.FeatureFlag`**: A nested record within the main `JourneySpecification`. It is null-safe and type-safe.
    ```java
    public record FeatureFlag(boolean enabled, Set<UserGroup> pilotGroups) {
        // Compact constructor ensures pilotGroups is never null.
        public FeatureFlag {
            pilotGroups = (pilotGroups == null) ? Set.of() : pilotGroups;
        }
    }
    ```
*   **`CachePolicy.java`**: A domain object that defines the configuration for a cache, moving policy from infrastructure into the core.
    ```java
    public record CachePolicy(String name, long ttlSeconds, long maxSize) {}
    ```
*   **`ValidationRule.java`**: A functional interface defining the contract for our extensible rule engine.
    ```java
    @FunctionalInterface
    public interface ValidationRule {
        boolean isSatisfied(User user, JourneySpecification spec);
    }
    ```

### 3.2. Application Layer Components

These components orchestrate the domain logic.

*   **`UserContextManager.java`**: Manages the `User` object for the duration of a request using a `ScopedValue`, consistent with our existing context management patterns.
*   **`ValidationRuleRegistry.java`**: A service locator that maps a `UserGroup` enum to a specific `ValidationRule` implementation. For any group without a custom rule, it provides a default implementation that checks for basic group membership.

### 3.3. Ports (The Hexagonal Boundaries)

These interfaces define the contracts between the application core and the infrastructure layer.

*   **`NamedGroupProviderPort.java`**: Defines the contract for fetching user group data. It includes methods for both lazy-loading a single user's groups and bulk-loading groups for the startup cache warmer.
    ```java
    public interface NamedGroupProviderPort {
        Set<UserGroup> getGroupsForUser(String userId);
        Map<UserGroup, Set<String>> getUsersForGroups(Set<UserGroup> groupNames);
    }
    ```
*   **`CacheProviderPort.java`**: A factory port that provides `CachePort` instances configured according to a given `CachePolicy`.
*   **`CachePort.java`**: A generic interface for cache operations (`get`, `putAll`).

### 3.4. Infrastructure Layer - Adapters

These are the concrete implementations of the ports.

*   **`CaffeineCacheProviderAdapter.java`**: The implementation of `CacheProviderPort`. It is a factory that creates and configures `CaffeineCacheAdapter` instances based on a `CachePolicy` from the core.
*   **`CaffeineCacheAdapter.java`**: The "dumb" implementation of `CachePort`. It simply wraps a pre-configured Caffeine cache instance and executes the policy it was given.
*   **`ConfigBasedNamedGroupProviderAdapter.java`**: The implementation of `NamedGroupProviderPort`.
    *   It depends on the `CacheProviderPort` to get a cache instance, ensuring decoupling.
    *   It loads a mapping of `UserGroup` enums to `Set<String>` of user IDs from a configuration source (e.g., a YAML file or database). This ensures all group mapping is type-safe.
*   **`PilotGroupCacheWarmer.java`**: An `ApplicationListener` that executes on startup.
    1.  It scans all `JourneySpecification` blueprints to find all unique `pilotGroups` required by the application.
    2.  It makes a single, bulk call to `namedGroupProvider.getUsersForGroups()` to fetch all relevant user data at once.
    3.  It then inverts this data and uses `groupProviderAdapter.prePopulateCache()` to fully warm the user-to-groups cache.

### 3.5. Infrastructure Layer - Middleware

This is the runtime engine that enforces the feature flag rules in the correct order.

1.  **`SecurityContextMiddleware` (`@Order(1)`)**:
    *   Executes first.
    *   Extracts the `userId` from the request's security token.
    *   Creates a **partially hydrated** `User` object containing only the `userId`.
    *   Binds this partial `User` to a `ScopedValue` via the `UserContextManager`. This ensures the expensive group lookup is deferred.

2.  **`JourneyContextMiddleware` (`@Order(2)`)**:
    *   Our existing middleware that loads the `JourneySpecification` for the current request and binds it to its `ScopedValue`.

3.  **`FeatureFlagMiddleware` (`@Order(3)`)**:
    *   Retrieves the `JourneySpecification` and checks if a `featureFlag` with `pilotGroups` is defined. If not, the journey is public, and the chain proceeds.
    *   If the flag is present, it checks the `enabled` status, throwing a `FeatureDisabledException` if `false`.
    *   It retrieves the partial `User` from the `UserContext`.
    *   **Lazy Loading**: It now makes a call to `namedGroupProvider.getGroupsForUser(userId)`. This call is served instantly from the pre-warmed cache, involving zero runtime I/O.
    *   It creates a **fully hydrated** `User` object with the retrieved groups.
    *   **Extensible Validation**: It iterates through the journey's required `pilotGroups`. For each group, it fetches the corresponding `ValidationRule` from the `ValidationRuleRegistry`.
    *   It executes the rule (`rule.isSatisfied(fullUser, spec)`). If *any* of the required rules return `true`, the user is authorized, and the chain proceeds.
    *   If no rules are satisfied, it throws a `FeatureNotAvailableForUserException`.

## 4. Conclusion

This design provides a complete, robust, and high-performance feature flagging system. It is fully type-safe, adheres to the strict separation of concerns demanded by our hexagonal architecture, and provides a powerful, extensible rule engine for complex authorization scenarios. It achieves this without compromising the performance of the request hot path. This is the definitive standard for operational control in our application.
