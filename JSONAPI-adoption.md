# Architectural Adoption Plan: JSON:API Query Layer

## 1. Objective

This document provides the definitive, unabridged implementation plan for integrating a standardized, reusable JSON:API-compliant query layer into the `book-transfers` service. The primary goals are to introduce advanced filtering and pagination capabilities while rigorously adhering to the existing Hexagonal Architecture, CQRS pattern, and the principle of non-invasive infrastructure.

## 2. Confirmed Understanding of Current State

A thorough analysis of the `book-transfers` service confirms the following:

- **Architecture:** The project is a multi-module Maven build (`core`, `infrastructure`, `app`) that correctly implements the principles of Hexagonal Architecture.
- **CQRS Pattern:** A clear separation of concerns exists. Commands are handled via a `CommandBus`, while queries are handled by a dedicated `PaymentQueryUseCase`.
- **Query Stack:** The current query stack is simple and direct:
    1.  **Adapter:** `BookTransferController` receives HTTP requests.
    2.  **Port:** It calls methods on the `PaymentQueryUseCase` input port (e.g., `findByReference`).
    3.  **Implementation:** The `MongoPaymentQueryHandler` (an output adapter) implements the port.
    4.  **Persistence:** It uses `MongoTemplate` to execute simple, non-paginated queries against a MongoDB collection, mapping results directly to a `PaymentView` read model.
- **Gap:** The current implementation lacks any mechanism for complex, multi-field filtering or for pagination.

## 3. Architectural Strategy

The chosen strategy is to introduce a generic, reusable Input Adapter that translates the JSON:API query syntax into a clean, domain-agnostic representation before it reaches the application's core.

1.  **The Resolver (`JsonApiArgumentResolver`):** A Spring `HandlerMethodArgumentResolver` will intercept incoming requests annotated with a custom `@JsonApiQuery` annotation. It will parse JSON:API filter and pagination parameters (`filter[field][op]=value`, `page[number]=N`, `page[size]=M`).
2.  **The Domain Contract (`DomainQuery`):** The resolver will produce a simple, immutable `DomainQuery` record containing a `cz.jirutka.rsql.parser.ast.Node` for the filter Abstract Syntax Tree (AST) and a Spring `Pageable` object for pagination. This object is the clean contract passed to the application's core.
3.  **The Persistence Translator:** A new dedicated component in the persistence layer will be responsible for translating the RSQL `Node` from the `DomainQuery` into a database-specific query object (i.e., a MongoDB `Criteria`).
4.  **The Assembler (`PaymentAssembler`):** A HATEOAS `RepresentationModelAssemblerSupport` will be used in the web adapter to transform the `Page<PaymentView>` returned from the core into a fully compliant JSON:API response, complete with `data`, `links`, and `meta` objects.

This strategy ensures that the core domain remains completely decoupled from HTTP and the JSON:API specification.

## 4. Unabridged Implementation Plan

The implementation will proceed in four distinct, verifiable stages.

### Stage 1: Establish the Generic Infrastructure

This stage involves adding the reusable framework components to the `infrastructure` module. These components are generic and have no specific knowledge of the "payment" feature.

1.  **Update Dependencies:** Add the following to `book-transfers/infrastructure/pom.xml`:
    ```xml
    <dependency>
        <groupId>com.toedter</groupId>
        <artifactId>spring-hateoas-jsonapi</artifactId>
        <version>2.1.0</version>
    </dependency>
    <dependency>
        <groupId>cz.jirutka.rsql</groupId>
        <artifactId>rsql-parser</artifactId>
        <version>2.1.0</version>
    </dependency>
    ```

2.  **Create Infrastructure Package:** Create a new package: `dexter.banking.booktransfers.infrastructure.adapter.in.web.jsonapi`.

3.  **Create Generic Framework Files:** Create the following files within the new `jsonapi` package. The content for these files should be copied directly from the `JSONAPI-filter-and-pagination.md` blueprint, as they are generic.

    - `DomainQuery.java`
    - `JsonApiController.java`
    - `JsonApiQuery.java`
    - `JsonApiRsqlParser.java`
    - `JsonApiArgumentResolver.java`
    - `JsonApiWebConfig.java`
    - `JsonApiErrorHandler.java`

**Verification for Stage 1:** The project compiles successfully. The JSON:API infrastructure is available in the application context but is not yet active.

### Stage 2: Extend the Core Application Port

This stage modifies the core domain contract to support searchable queries.

1.  **Update Use Case Port:** Modify the `PaymentQueryUseCase` interface in the `core` module to include a new `search` method.

    **File:** `book-transfers/core/src/main/java/dexter/banking/booktransfers/core/port/in/payment/PaymentQueryUseCase.java`
    **Action:** Add the following method signature:
    ```java
    import org.springframework.data.domain.Page;
    import dexter.banking.booktransfers.infrastructure.adapter.in.web.jsonapi.DomainQuery; // Note: This is a temporary coupling, will be fixed by moving DomainQuery

    // ... inside interface
    Page<PaymentView> search(DomainQuery query);
    ```
    *(Correction during implementation: To maintain architectural purity, the `DomainQuery` class, which is part of the port's contract, should be moved from the `infrastructure` package to a `core.port.in.query` package or similar shared location.)*

**Verification for Stage 2:** The `PaymentQueryUseCase` interface now correctly defines the contract for a paginated search. The `MongoPaymentQueryHandler` will have a compilation error, which will be resolved in the next stage.

### Stage 3: Implement the Persistence Adapter

This stage implements the logic to handle the new `search` contract in the MongoDB persistence layer.

1.  **Create RSQL to MongoDB Converter:** To translate the RSQL AST to a MongoDB query, we will create a dedicated visitor implementation.

    **File:** `book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/out/persistence/mongo/RsqlMongoVisitor.java`
    **Action:** Create the following class:
    ```java
    package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.mongo;

    import cz.jirutka.rsql.parser.ast.*;
    import org.springframework.data.mongodb.core.query.Criteria;

    public class RsqlMongoVisitor extends NoArgRSQLVisitorAdapter<Criteria> {

        @Override
        public Criteria visit(AndNode node) {
            return new Criteria().andOperator(
                node.getChildren().stream().map(n -> n.accept(this)).toList()
            );
        }

        @Override
        public Criteria visit(OrNode node) {
            return new Criteria().orOperator(
                node.getChildren().stream().map(n -> n.accept(this)).toList()
            );
        }

        @Override
        public Criteria visit(ComparisonNode node) {
            return switch (node.getOperator().getSymbol()) {
                case "==" -> Criteria.where(node.getSelector()).is(getParsedValue(node));
                case "!=" -> Criteria.where(node.getSelector()).ne(getParsedValue(node));
                case "=gt=" -> Criteria.where(node.getSelector()).gt(getParsedValue(node));
                case "=ge=" -> Criteria.where(node.getSelector()).gte(getParsedValue(node));
                case "=lt=" -> Criteria.where(node.getSelector()).lt(getParsedValue(node));
                case "=le=" -> Criteria.where(node.getSelector()).lte(getParsedValue(node));
                // Add more operators like 'in', 'nin', 'like' as needed
                default -> throw new UnsupportedOperationException("Operator not supported: " + node.getOperator().getSymbol());
            };
        }

        private Object getParsedValue(ComparisonNode node) {
            // Basic type inference. A more robust solution might be needed for dates, UUIDs etc.
            String value = node.getArguments().get(0);
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                // Not a long
            }
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e) {
                // Not a double
            }
            return value;
        }
    }
    ```

2.  **Update the Query Handler:** Modify `MongoPaymentQueryHandler` to implement the `search` method.

    **File:** `book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/out/persistence/payment/mongo/MongoPaymentQueryHandler.java`
    **Action:**
    - Implement the `search` method from the interface.
    - Use the new `RsqlMongoVisitor` to build the criteria.
    - Use `PageableExecutionUtils` to correctly construct a `Page`.

    ```java
    // Add these imports
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.data.support.PageableExecutionUtils;
    import dexter.banking.booktransfers.infrastructure.adapter.in.web.jsonapi.DomainQuery;
    import cz.jirutka.rsql.parser.ast.Node;

    // ... inside the class

    @Override
    public Page<PaymentView> search(DomainQuery domainQuery) {
        Pageable pageable = domainQuery.pageable();
        Query query = new Query().with(pageable);
        addProjection(query);

        if (domainQuery.filterNode() != null) {
            Criteria criteria = domainQuery.filterNode().accept(new RsqlMongoVisitor());
            query.addCriteria(criteria);
        }

        List<PaymentView> results = mongoTemplate.find(query, PaymentView.class, TRANSACTION_COLLECTION);

        return PageableExecutionUtils.getPage(results, pageable,
                () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), TRANSACTION_COLLECTION));
    }
    ```

**Verification for Stage 3:** The project compiles. The persistence layer is now fully capable of executing paginated, multi-field filter queries based on the `DomainQuery` object.

### Stage 4: Implement the Feature's Input Adapter

This final stage exposes the new search capability via a clean, JSON:API-compliant REST endpoint.

1.  **Create Payment DTO:** Create a DTO for the web layer.

    **File:** `book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/in/web/PaymentDto.java`
    **Action:**
    ```java
    package dexter.banking.booktransfers.infrastructure.adapter.in.web;

    import com.toedter.spring.hateoas.jsonapi.JsonApiId;
    import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;
    import lombok.Data;
    import org.springframework.hateoas.RepresentationModel;
    import java.util.UUID;

    @Data
    @JsonApiTypeForClass("payments")
    public class PaymentDto extends RepresentationModel<PaymentDto> {
        @JsonApiId
        private UUID transactionId;
        private String transactionReference;
        private String status;
        private String state;
    }
    ```

2.  **Update Web Mapper:** Add a method to `WebMapper` to map `PaymentView` to `PaymentDto`.

    **File:** `book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/in/web/WebMapper.java`
    **Action:** Add the following method to the interface:
    ```java
    PaymentDto toDto(PaymentView paymentView);
    ```

3.  **Create Payment Assembler:** This component builds the final JSON:API response structure.

    **File:** `book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/in/web/PaymentAssembler.java`
    **Action:**
    ```java
    package dexter.banking.booktransfers.infrastructure.adapter.in.web;

    import dexter.banking.booktransfers.core.application.payment.query.PaymentView;
    import org.springframework.data.domain.Page;
    import org.springframework.data.web.PagedResourcesAssembler;
    import org.springframework.hateoas.EntityModel;
    import org.springframework.hateoas.PagedModel;
    import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
    import org.springframework.stereotype.Component;
    import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

    import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
    import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

    @Component
    public class PaymentAssembler extends RepresentationModelAssemblerSupport<PaymentView, EntityModel<PaymentDto>> {

        private final WebMapper mapper;
        private final PagedResourcesAssembler<PaymentView> pagedAssembler;

        public PaymentAssembler(WebMapper mapper, PagedResourcesAssembler<PaymentView> pagedAssembler) {
            super(BookTransferController.class, (Class<EntityModel<PaymentDto>>)(Class<?>)EntityModel.class);
            this.mapper = mapper;
            this.pagedAssembler = pagedAssembler;
        }

        @Override
        public EntityModel<PaymentDto> toModel(PaymentView entity) {
            PaymentDto dto = mapper.toDto(entity);
            return EntityModel.of(dto,
                linkTo(methodOn(BookTransferController.class).getTransactionInfo(entity.getTransactionId())).withSelfRel()
            );
        }

        public PagedModel<EntityModel<PaymentDto>> toPagedModel(Page<PaymentView> page) {
            if (page.isEmpty()) {
                return (PagedModel<EntityModel<PaymentDto>>) pagedAssembler.toEmptyModel(page, PaymentDto.class);
            }
            return pagedAssembler.toModel(page, this,
                ServletUriComponentsBuilder.fromCurrentRequest()::build
            );
        }
    }
    ```

4.  **Update Controller:** Finally, add the new search endpoint to `BookTransferController`.

    **File:** `book-transfers/infrastructure/src/main/java/dexter/banking/booktransfers/infrastructure/adapter/in/web/BookTransferController.java`
    **Action:**
    - Annotate the class with `@JsonApiController`.
    - Inject the `PaymentAssembler`.
    - Add the new `search` endpoint method.

    ```java
    // At class level
    @JsonApiController
    @RequestMapping(path = "/api", produces = "application/vnd.api+json") // Set default produce type

    // ... inside class
    private final PaymentAssembler paymentAssembler; // Add to constructor

    @GetMapping({"/v1/book-transfers/payments", "/v2/book-transfers/payments"})
    public ResponseEntity<PagedModel<EntityModel<PaymentDto>>> searchPayments(@JsonApiQuery DomainQuery query) {
        Page<PaymentView> results = paymentQueryUseCase.search(query);
        return ResponseEntity.ok(paymentAssembler.toPagedModel(results));
    }

    // IMPORTANT: The old findByReference method should be removed or deprecated to avoid ambiguity.
    // The new endpoint `GET /v1/book-transfers/payments?filter[transactionReference]=='some-ref'` replaces it.
    ```

**Verification for Stage 4:** The application starts. A `GET` request to `/api/v1/book-transfers/payments?filter[status]=='COMPLETED'&page[number]=1&page[size]=5` returns a 200 OK with a valid JSON:API response body containing filtered and paginated data, including `links` for pagination.

## 5. Conclusion

This plan provides a complete, end-to-end guide for integrating a powerful and standardized query mechanism into the `book-transfers` service. By following these stages, we will enhance the service's capabilities significantly while maintaining strict architectural boundaries and ensuring the core domain remains pure and isolated from infrastructure concerns.
