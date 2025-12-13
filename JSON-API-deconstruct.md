# Implementation Plan: Deconstructing JSON:API Support

## 1. Objective

Refactor the current monolithic `DomainQuery` argument resolution into a granular, library-esque set of components. The goal is to provide independent, pluggable support for JSON:API Filtering, Sorting, and Pagination, allowing controllers to mix and match these features as needed.

## 2. Architectural Vision

Instead of a single `@JsonApiQuery DomainQuery` parameter, controllers will be able to use:

```java
public ResponseEntity<...> list(
    @JsonApiFilter Node filter,       // Resolves filter[...] to RSQL Node
    @JsonApiSort Sort sort,           // Resolves sort=... to Spring Sort
    @JsonApiPage Pageable pageable    // Resolves page[...] to Spring Pageable
)
```

This aligns with Spring's composable architecture and allows for greater flexibility (e.g., an endpoint that supports filtering but not pagination).

## 3. Components to Implement

### 3.1. Annotations
We will define custom annotations to trigger the specific resolvers.

*   `@JsonApiFilter`: Marks a parameter (typically `Node` or `String`) to be resolved from `filter[...]` query parameters.
*   `@JsonApiSort`: Marks a `Sort` parameter to be resolved from the `sort` query parameter.
*   `@JsonApiPage`: Marks a `Pageable` parameter to be resolved from `page[...]` query parameters.

### 3.2. Resolvers
We will implement `HandlerMethodArgumentResolver` for each concern.

1.  **`JsonApiFilterArgumentResolver`**:
    *   **Input:** HTTP Request parameters starting with `filter[`.
    *   **Logic:** Uses `JsonApiRsqlParser` to convert the map of filters into a single RSQL `Node`.
    *   **Output:** `cz.jirutka.rsql.parser.ast.Node`.

2.  **`JsonApiSortArgumentResolver`**:
    *   **Input:** HTTP Request parameter `sort`.
    *   **Logic:** Parses comma-separated fields. Handles `-` prefix for descending order.
    *   **Output:** `org.springframework.data.domain.Sort`.

3.  **`JsonApiPageableArgumentResolver`**:
    *   **Input:** HTTP Request parameters `page[number]` and `page[size]`.
    *   **Logic:** Delegates to or extends Spring's `PageableHandlerMethodArgumentResolver` but configured specifically for JSON:API standards (1-based indexing).
    *   **Output:** `org.springframework.data.domain.Pageable`.

### 3.3. Configuration
*   **`JsonApiConfig`**: Update the configuration to register these three new resolvers and remove the old `JsonApiArgumentResolver`.

## 4. Migration Steps

1.  **Create Annotations:** Define `@JsonApiFilter`, `@JsonApiSort`, `@JsonApiPage`.
2.  **Implement Resolvers:** Create the three resolver classes in `dexter.banking.limit.config`.
3.  **Refactor Controller:** Update `PayeeController.list` to use the new granular arguments instead of `DomainQuery`.
4.  **Refactor Repository:** Update `PayeeRepository.findAll` to accept `Node`, `Sort`, and `Pageable` directly (or a cleaner wrapper if needed, but likely direct arguments are fine).
5.  **Cleanup:** Delete `DomainQuery`, `JsonApiQuery`, and `JsonApiArgumentResolver`.

## 5. Verification

*   **Filter Test:** `GET /payees?filter[name]=Payee 1`
*   **Sort Test:** `GET /payees?sort=-name`
*   **Page Test:** `GET /payees?page[number]=2&page[size]=5`
*   **Combined Test:** `GET /payees?filter[name]=Payee&sort=id&page[number]=1`

This approach creates a reusable "kit" of parts that can be lifted and shifted to other services (like `book-transfers-service`) as a shared library.