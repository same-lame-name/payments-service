# JSON:API Implementation Strategy Review

## Executive Summary

This document outlines the specific elements of the [JSON:API Specification (v1.0/v1.1)](https://jsonapi.org/) selected for adoption within the `book-transfers` service and the generic infrastructure layer. The implementation strategy strictly adheres to Hexagonal Architecture principles, ensuring the core domain remains agnostic of the HTTP/JSON:API wire format.

## 1. Content Negotiation

**JSON:API Element:** `Client and Server Responsibilities`
*   **Requirement:** Clients and Servers must exchange documents using the specific media type `application/vnd.api+json`.

**Implementation Strategy:**
*   **Configuration:** `JsonApiWebConfig` bean registers `JsonApiMediaTypeConfiguration`.
*   **Controller Enforcement:** The `@JsonApiController` and `@RequestMapping` annotations explicitly set `produces = "application/vnd.api+json"`.
*   **Library Support:** Leveraging `com.toedter:spring-hateoas-jsonapi` to automatically handle the serialization of Java objects into the correct MIME type structure.

## 2. Fetching Data: Filtering

**JSON:API Element:** `Filtering`
*   **Requirement:** The `filter` query parameter family is reserved for filtering data. The specification is agnostic regarding the syntax within the value, but recommends a structure like `filter[attribute][operator]=value`.

**Implementation Strategy:**
*   **Input Parsing:** A custom `JsonApiArgumentResolver` intercepts the HTTP request.
*   **Syntax Translation:** `JsonApiRsqlParser` parses parameters matching `filter[field][op]` into an RSQL Abstract Syntax Tree (AST).
*   **Domain Contract:** The AST is encapsulated in a clean `DomainQuery` record (containing `cz.jirutka.rsql.parser.ast.Node`), which is passed to the Input Port.
*   **Persistence Translation:** A `RsqlMongoVisitor` (Infrastructure Adapter) translates the RSQL AST into MongoDB `Criteria` objects at the persistence layer.

## 3. Fetching Data: Pagination

**JSON:API Element:** `Pagination`
*   **Requirement:** The `page` query parameter family is reserved for pagination.
*   **Specifics:** Adoption of a page-based strategy using `page[number]` and `page[size]`.

**Implementation Strategy:**
*   **Input Parsing:** The `JsonApiArgumentResolver` delegates to Spring's native `PageableHandlerMethodArgumentResolver`.
*   **Configuration:** The resolver is explicitly configured to map `page[number]` and `page[size]` to Spring's `Pageable` object and enforce 1-based indexing (standard for JSON:API).
*   **Domain Contract:** The `Pageable` object is passed within the `DomainQuery` record.
*   **Execution:** `PageableExecutionUtils` is used in the persistence adapter to execute the query and count total results.

## 4. Document Structure

**JSON:API Element:** `Top Level` & `Resource Objects`
*   **Requirement:** A document must contain at least one of `data`, `errors`, or `meta`. Resource objects must contain `id` and `type`.

**Implementation Strategy:**
*   **DTO Definition:** `PaymentDto` is annotated with `@JsonApiTypeForClass("payments")` to define the `type`.
*   **Identity:** The `@JsonApiId` annotation on `PaymentDto.transactionId` maps the unique identifier.
*   **Assembly:** The `PaymentAssembler` (extending `RepresentationModelAssemblerSupport`) converts domain entities into `EntityModel<PaymentDto>`.
*   **Serialization:** The `spring-hateoas-jsonapi` library automatically wraps these models in the standard `data` envelope.

## 5. HATEOAS & Links

**JSON:API Element:** `Links`
*   **Requirement:** Responses should contain links to related resources. Paginated responses must contain `first`, `last`, `prev`, and `next` links.

**Implementation Strategy:**
*   **Resource Links:** `PaymentAssembler` adds `self` links to individual resources using `WebMvcLinkBuilder`.
*   **Pagination Links:** `PagedResourcesAssembler` is used to automatically generate navigation links (`first`, `next`, etc.) based on the `Page` object returned by the domain.
*   **Context Preservation:** `ServletUriComponentsBuilder` is used to ensure that existing filter parameters are preserved in the generated pagination links (e.g., clicking "Next" keeps the active filters).

## 6. Error Handling

**JSON:API Element:** `Errors`
*   **Requirement:** Error responses must be sent with the appropriate HTTP status code and a document containing an `errors` array.

**Implementation Strategy:**
*   **Interception:** A `@ControllerAdvice` annotated class `JsonApiErrorHandler` targets controllers annotated with `@JsonApiController`.
*   **Formatting:** Exceptions are mapped to `JsonApiError` objects (containing `status`, `title`, `detail`).
*   **Response:** These are wrapped in a `JsonApiErrors` collection and returned with the correct HTTP status code.

## Summary of Architectural Flow

| Layer | Component | Responsibility |
| :--- | :--- | :--- |
| **Web (Infra)** | `JsonApiArgumentResolver` | Translates `HTTP` -> `DomainQuery` |
| **Core (Domain)** | `PaymentQueryUseCase` | Accepts `DomainQuery`, returns `Page<PaymentView>` |
| **Persistence (Infra)** | `RsqlMongoVisitor` | Translates `DomainQuery` -> `Mongo Criteria` |
| **Web (Infra)** | `PaymentAssembler` | Translates `Page<PaymentView>` -> `JSON:API Document` |

---
*Generated by Gemini Code Assist based on architectural blueprints.*