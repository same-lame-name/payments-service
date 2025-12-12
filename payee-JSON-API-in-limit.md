# Implementation Plan: JSON:API POC in Limit Management Service

## 1. Objective

Establish a Proof of Concept (POC) for JSON:API adoption within the `limit-management-service`. This POC focuses on the fundamental structural elements: Controller definition, Resource Document structure, and Pagination.

**Goal:** Validate the "Skeleton" of JSON:API (Wire format + Pagination) before implementing complex features like Filtering or Error Handling.

## 2. Scope & Constraints

*   **Target Service:** `limit-management-service` (Layered Architecture).
*   **Resource:** `Payee` (A new, isolated resource for this POC).
*   **Persistence:** In-Memory (Mock Repository) to isolate the API layer testing.
*   **Included Features:**
    *   **Content Negotiation:** `application/vnd.api+json`.
    *   **Document Structure:** Standard `data`, `attributes`, `id`, `type` envelope.
    *   **Pagination:** `page[number]` and `page[size]` using Spring HATEOAS.
*   **Excluded Features (Deferred):**
    *   Filtering (`filter[...]`).
    *   Sorting (`sort`).
    *   Complex Error Handling (Standard Spring Boot errors for now).
    *   Relationships/Inclusions (`include`).

## 3. Implementation Steps

### Step 1: Dependencies

We need to add the library that handles the JSON:API serialization and deserialization.

*   **Action:** Add `spring-hateoas-jsonapi` to `pom.xml`.

```xml
<dependency>
    <groupId>com.toedter</groupId>
    <artifactId>spring-hateoas-jsonapi</artifactId>
    <version>2.1.0</version>
</dependency>
```

### Step 2: Domain & Resource Definition

We will define the internal domain entity and the external JSON:API resource (DTO).

*   **Domain Entity:** `Payee` (Simple POJO with `id`, `name`, `iban`).
*   **Resource DTO:** `PayeeDto`.
    *   **Annotations:**
        *   `@JsonApiTypeForClass("payees")`: Defines the `type` field in the JSON document.
        *   `@JsonApiId`: Maps the identifier.
    *   **Structure:** Extends `RepresentationModel<PayeeDto>`.

### Step 3: In-Memory Persistence (Mock)

To test pagination effectively without database complexity, we will create a dummy repository.

*   **Component:** `PayeeRepository`.
*   **Storage:** `private final List<Payee> payees = new CopyOnWriteArrayList<>();`
*   **Initialization:** Pre-load 50-100 dummy payees in the constructor so we can test pagination immediately.
*   **Methods:**
    *   `Payee save(Payee payee)`
    *   `Page<Payee> findAll(Pageable pageable)`: Manually implement sub-list logic to simulate pagination.

### Step 4: Infrastructure Configuration

We need to configure Spring MVC to understand JSON:API pagination parameters, which differ from Spring's defaults.

*   **Component:** `JsonApiConfig` (implements `WebMvcConfigurer`).
*   **Pagination Config:**
    *   Override `PageableHandlerMethodArgumentResolver`.
    *   Set page parameter name to `page[number]`.
    *   Set size parameter name to `page[size]`.
    *   **Crucial:** Enable one-indexed parameters (`setOneIndexedParameters(true)`) as JSON:API standard suggests page 1 is the first page, not 0.
*   **Media Type:** Register `JsonApiMediaTypeConfiguration` to handle the `application/vnd.api+json` content type.

### Step 5: The Controller

This is the core of the POC.

*   **Component:** `PayeeController`.
*   **Base Path:** `/api/v1/payees`.
*   **Produces:** `application/vnd.api+json`.
*   **Endpoints:**
    1.  **Create Payee (POST):**
        *   Accepts: `EntityModel<PayeeDto>`.
        *   Logic: Unwrap DTO -> Convert to Domain -> Save -> Convert to DTO -> Wrap in `EntityModel`.
        *   Returns: `201 Created` with the created resource.
    2.  **List Payees (GET):**
        *   Accepts: `Pageable` (automatically resolved from `page[number]`/`page[size]`).
        *   Logic: Repo.findAll(pageable) -> Returns `Page<Payee>`.
        *   Response: Use `PagedResourcesAssembler<Payee>` to convert the `Page` into a `PagedModel`. This automatically generates `first`, `last`, `next`, `prev` links.

## 4. Verification Plan

Once implemented, we will verify using the following requests:

**1. Create a Payee:**
```http
POST /api/v1/payees
Content-Type: application/vnd.api+json
Body: { "data": { "type": "payees", "attributes": { "name": "John Doe", "iban": "DE123456" } } }
```

**2. Get Paginated List:**
```http
GET /api/v1/payees?page[number]=2&page[size]=5
```
*Expectation:* Response contains `data` (array of 5 items), `meta` (page info), and `links` (pointing to page 1 and 3).