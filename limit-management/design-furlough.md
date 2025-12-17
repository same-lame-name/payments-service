Architectural Directive: Pipeline-Orchestrator & Strategy-Registry Pattern
1. Context & Motivation

Current State: The application currently relies on a layered architecture where Controllers delegate directly to "Fat Services." These services are currently burdened with cross-cutting concerns (serialization, configuration loading, validation, enrichment, business rules) alongside their core execution logic.

Problem Statement: This violates the Single Responsibility Principle (SRP). As the application scales (serving multiple markets/variants), this coupling makes it difficult to maintain, test, and extend. We need to decouple the "Pre-processing" (Steps 1-6) from the "Core Execution" (Step 7).

Target Architecture: We are migrating to a Pipeline-Orchestrator Pattern for request processing, followed by a Map-Based Strategy Registry for execution.

    Why: To ensure every request goes through a standard, ordered lifecycle of validation and enrichment before it reaches the core logic.

    Goal: Streamline the flow so that Controllers are "dumb," Orchestrators manage the flow, Middleware handles pre-processing, and Strategies handle execution.

2. Design Specification (The "What" and "How")

We are implementing two distinct phases:
Phase 1: The Pre-Processing Pipeline

An ordered chain of middleware components that mutate/enrich the Request Object.

    Mechanism: Spring Ordered interface or @Order annotation.

    Input: A BaseRequest object.

    Behavior: Each step performs a specific task (e.g., Load Config, Validate Fields, Fetch Data) and populates the BaseRequest.

    Failure: If a step fails (e.g., Validation Error), it throws an exception, halting the pipeline immediately.

Phase 2: The Execution Strategy (Registry Pattern)

A safe, high-performance routing mechanism to select the correct service implementation.

    Mechanism: A Registry component that builds a Map<String, Strategy> at application startup.

    Lookup: O(1) retrieval using a Key derived from the Request.

    Safety: The Registry must Fail-Fast at startup if two strategies claim the same Key (Collision Detection).

    Flexibility: A single Strategy class must be able to claim multiple keys (One-to-Many).

3. The Core Contracts (Interfaces)

The Agent must generate/refactor code based on these exact definitions:
A. The Pipeline Step
Java

public interface PipelineMiddleware extends org.springframework.core.Ordered {
// Defines the order of execution (Low = First)
int getOrder();

    // Executes logic and enriches the request in-place
    void process(BaseRequest request);
}

B. The Strategy Interface
Java

public interface ExecutionStrategy {
// Returns a Set of keys this service handles (e.g., "US-TAX", "CA-TAX")
Set<String> getSupportedIdentifiers();

    // The core business logic
    ResponseDto execute(BaseRequest request);
}

C. The Request Object (Context)

The DTO must be capable of holding enriched data and determining its own routing key.
Java

public abstract class BaseRequest {
// Input fields...

    // Enriched fields (Config, UserProfile, etc.)
    
    // Abstract method for Strategy Resolution
    public abstract String getJourneyIdentifier();
}

4. The Request Lifecycle (Step-by-Step)

The Agent must understand that the Controller delegates to the Orchestrator, which runs the following flow:

    Ingestion: Request received by Controller -> Passed to PipelineOrchestrator.

    Pipeline Step 1 (Config): Load dynamic configuration based on request attributes.

    Pipeline Step 2 (Validation): Validate mandatory fields defined by the loaded config.

    Pipeline Step 3 (Enrichment): Fetch auxiliary data (DB, External APIs) and attach to Request.

    Pipeline Step 4 (Rules): Execute business rules (Eligibility, Limits) against enriched data.

    Routing (The Handover): The Orchestrator calls request.getJourneyIdentifier() to get the Key.

    Resolution: The Orchestrator asks the StrategyRegistry for the handler associated with the Key.

    Execution: The resolved ExecutionStrategy runs the core logic.

5. Technical Roadmap (Future Context)

Note for the Agent: While generating the initial code, structure it to be compatible with these future optimizations (do not implement them yet, but keep the design open):

    Step 3 (Enrichment): Will eventually use CompletableFuture or Virtual Threads for parallel data fetching.

    Context Passing: May eventually use Scoped Values (Project Loom) instead of mutating the Request object, though for now, mutation is acceptable.

    Validation: Will leverage JSR-380 (Bean Validation) dynamically.

6. Immediate Action Items for the Agent

   Scaffold the Interfaces: Create PipelineMiddleware, ExecutionStrategy, and the BaseRequest abstract class.

   Implement the Registry: Create StrategyRegistry. It must:

        Inject List<ExecutionStrategy>.

        Iterate and populate a HashMap.

        Throw IllegalStateException if a duplicate key is detected during startup.

   Implement the Orchestrator: Create PipelineOrchestrator that injects List<PipelineMiddleware> and the StrategyRegistry.

   Refactor the Controller: Change the controller to call orchestrator.handle(request).

Constraint: Do not use "supports(DTO)" logic for the strategy. Use the Map-based Registry approach defined above to ensure O(1) lookup and startup safety.