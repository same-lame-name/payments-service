Architectural Blueprint: JSON:API Integration for Hexagonal Architecture

1. Executive Summary

This document serves as the definitive architectural blueprint and implementation guide for introducing a JSON:API compliant Input Adapter into an existing Hexagonal Architecture application (specifically the Book Transfers service).

The design prioritizes Architectural Purity and Non-Invasiveness. It strictly separates the Protocol (JSON:API wire format) from the Domain Logic, ensuring that the Core Domain remains agnostic of HTTP, JSON, or specific filtering syntaxes.

2. Architectural Rationale & Decision Record

2.1 The Problem Statement

Standard libraries for JSON:API (like Elide or Crnk) often couple the API layer directly to the Persistence layer (JPA), bypassing the Domain completely. This violates the Dependency Rule of Hexagonal Architecture. Furthermore, manually parsing complex filter strings (filter[amount][gt]=100) and managing pagination links in every Controller leads to code duplication and "Leaky Abstractions."

2.2 The Solution Strategy

We treat JSON:API as an Infrastructure Concern, handled entirely by a reusable Adapter Layer.

    Input Parsing (The "Resolver"): We use a custom Spring ArgumentResolver to intercept incoming HTTP requests. It translates JSON:API specific query parameters (Filtering, Pagination) into a technology-agnostic DomainQuery object using RSQL.

    Output Formatting (The "Converter"): We leverage the spring-hateoas-jsonapi library strictly as a Message Converter. It handles the "wrapping" (Envelopes) and "unwrapping" (Requests) automatically, leaving the Controller to work with clean DTOs.

    Link Generation (The "Assembler"): We extend Spring HATEOAS Assembler to generate strictly compliant Pagination Links (next, prev, first, last) that preserve all custom filter parameters automatically.

2.3 Technology Stack

    Structure: com.toedter:spring-hateoas-jsonapi (Structure & Wire Format)

    Logic: cz.jirutka.rsql:rsql-parser (Filtering Logic / AST)

    Mapping: org.mapstruct:mapstruct (Object-to-Object Mapping)

    Framework: Spring Boot 3 + Spring HATEOAS

3. High-Level Architecture Diagram

The following diagram illustrates the flow of data through the Input Adapter. Notice how the "Dirty" JSON:API specifics are stripped away before reaching the "Clean" Domain Port.

    Request: Client sends GET /transfers?filter[amount][gt]=10&page[number]=1.

    Resolver: Intercepts request. Uses RSQL to parse filters into an AST (Node). Uses Spring to parse Pageable. Produces DomainQuery.

    Controller: Receives DomainQuery. Calls Input Port.

    Domain: Executes logic. Returns Page<BookTransfer>.

    Assembler: Converts Domain Entities to TransferDto. Enriches with HATEOAS Links.

    Converter: Wraps DTO in JSON:API Envelope (data, links, meta).

4. Implementation Guide: The Infrastructure Layer

This code is generic and belongs in your com.bank.infra.jsonapi package. It is written once and used by all features.

4.1 Dependencies (pom.xml)

Ensure these dependencies are present in your module.
XML

<dependencies>
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
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>1.5.5.Final</version>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-hateoas</artifactId>
    </dependency>
</dependencies>

4.2 The Annotations (JsonApiQuery, JsonApiController)

These annotations make the framework Opt-In.
Java

package com.bank.infra.jsonapi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.web.bind.annotation.RestController;

// Triggers the custom Argument Resolver
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiQuery {
}

// Triggers the custom Error Handler
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RestController
public @interface JsonApiController {
}

4.3 The Domain Contract (DomainQuery)

This is the clean object your Use Cases will accept.
Java

package com.bank.infra.jsonapi;

import cz.jirutka.rsql.parser.ast.Node;
import org.springframework.data.domain.Pageable;

public record DomainQuery(
Node filterNode,
Pageable pageable
) {
public static DomainQuery empty() {
return new DomainQuery(null, Pageable.unpaged());
}
}

4.4 The Logic Engine (JsonApiRsqlParser)

Handles the translation of filter[key][op]=val to RSQL AST.
Java

package com.bank.infra.jsonapi;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JsonApiRsqlParser {

    private final RSQLParser rsqlParser = new RSQLParser();

    public Node parse(Map<String, String> params) {
        // Filter map for keys starting with "filter" and join them with RSQL AND (;)
        String rsqlString = params.entrySet().stream()
                .filter(e -> e.getKey().startsWith("filter"))
                .map(this::toRsqlFragment)
                .collect(Collectors.joining(";"));

        if (rsqlString == null || rsqlString.isBlank()) {
            return null;
        }
        return rsqlParser.parse(rsqlString);
    }

    private String toRsqlFragment(Map.Entry<String, String> entry) {
        String key = entry.getKey();
        String value = entry.getValue();

        // Parse: filter[amount][gt] -> amount
        int firstBracket = key.indexOf('[');
        int secondBracket = key.indexOf(']');
        String field = key.substring(firstBracket + 1, secondBracket);

        // Parse Operator: [gt]
        String op = "=="; // Default
        int thirdBracket = key.indexOf('[', secondBracket + 1);
        if (thirdBracket > 0) {
            int fourthBracket = key.indexOf(']', thirdBracket + 1);
            String opCode = key.substring(thirdBracket + 1, fourthBracket);
            op = mapOperator(opCode);
        }

        // Quote value to handle spaces safely in RSQL
        String safeValue = value.contains(" ") ? "'" + value + "'" : value;

        return field + op + safeValue;
    }

    private String mapOperator(String jsonApiOp) {
        return switch (jsonApiOp) {
            case "gt" -> "=gt=";
            case "lt" -> "=lt=";
            case "ge" -> "=ge=";
            case "le" -> "=le=";
            case "neq" -> "!=";
            case "like" -> "=="; // Wildcard handled by value content
            case "eq" -> "==";
            default -> "==";
        };
    }
}

4.5 The Orchestrator (JsonApiArgumentResolver)

This component bridges the gap between HTTP Strings and Java Objects.
Java

package com.bank.infra.jsonapi;

import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import java.util.HashMap;
import java.util.Map;

@Component
public class JsonApiArgumentResolver implements HandlerMethodArgumentResolver {

    private final JsonApiRsqlParser rsqlParser;
    private final PageableHandlerMethodArgumentResolver pageResolver;

    public JsonApiArgumentResolver(JsonApiRsqlParser rsqlParser) {
        this.rsqlParser = rsqlParser;
        
        // Enforce JSON:API Pagination Standards (1-based indexing)
        this.pageResolver = new PageableHandlerMethodArgumentResolver();
        this.pageResolver.setPageParameterName("page[number]");
        this.pageResolver.setSizeParameterName("page[size]");
        this.pageResolver.setSortParameterName("sort");
        this.pageResolver.setOneIndexedParameters(true); 
        this.pageResolver.setFallbackPageable(Pageable.ofSize(10));
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonApiQuery.class) 
            && parameter.getParameterType().equals(DomainQuery.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mav,
                                  NativeWebRequest webRequest, WebDataBinderFactory binder) {
        
        // 1. Delegate Pagination to Spring
        Pageable pageable = pageResolver.resolveArgument(parameter, mav, webRequest, binder);

        // 2. Delegate Filtering to RSQL Parser
        Map<String, String> params = new HashMap<>();
        webRequest.getParameterNames().forEachRemaining(name -> 
            params.put(name, webRequest.getParameter(name)));

        var filterNode = rsqlParser.parse(params);

        return new DomainQuery(filterNode, pageable);
    }
}

4.6 Configuration & Error Handling

These classes wire everything together and ensure strict error compliance.
Java

// JsonApiWebConfig.java
package com.bank.infra.jsonapi;

import com.toedter.spring.hateoas.jsonapi.JsonApiConfiguration;
import com.toedter.spring.hateoas.jsonapi.JsonApiMediaTypeConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.List;

@Configuration
public class JsonApiWebConfig implements WebMvcConfigurer {

    private final JsonApiArgumentResolver jsonApiArgumentResolver;

    public JsonApiWebConfig(JsonApiArgumentResolver jsonApiArgumentResolver) {
        this.jsonApiArgumentResolver = jsonApiArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(jsonApiArgumentResolver);
    }

    // Enable the library's MessageConverters
    @Bean
    public JsonApiMediaTypeConfiguration jsonApiMediaTypeConfiguration(
            ObjectProvider<JsonApiConfiguration> configuration,
            AutowireCapableBeanFactory beanFactory) {
        return new JsonApiMediaTypeConfiguration(configuration, beanFactory);
    }
}

Java

// JsonApiErrorHandler.java
package com.bank.infra.jsonapi;

import com.toedter.spring.hateoas.jsonapi.JsonApiError;
import com.toedter.spring.hateoas.jsonapi.JsonApiErrors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

// Only targets our specific controllers to be non-invasive
@ControllerAdvice(annotations = JsonApiController.class)
public class JsonApiErrorHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<JsonApiErrors> handleException(Exception ex) {
        JsonApiError error = JsonApiError.create()
                .withStatus(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))
                .withTitle("Internal Server Error")
                .withDetail(ex.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(JsonApiErrors.create().withError(error));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<JsonApiErrors> handleBadRequest(IllegalArgumentException ex) {
        JsonApiError error = JsonApiError.create()
                .withStatus(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                .withTitle("Bad Request")
                .withDetail(ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(JsonApiErrors.create().withError(error));
    }
}

5. Implementation Guide: The Feature Layer

This demonstrates how to implement the BookTransfer Input Adapter using the infrastructure above.

5.1 The Domain Layer (Context)

The Input Port (TransferUseCase) now accepts our DomainQuery.
Java

package com.bank.transfers.domain;

import com.bank.infra.jsonapi.DomainQuery;
import org.springframework.data.domain.Page;
import java.math.BigDecimal;

public record BookTransfer(String id, BigDecimal amount, String currency, String status) {}

public interface TransferUseCase {
Page<BookTransfer> search(DomainQuery query);
BookTransfer create(BookTransfer command);
BookTransfer get(String id);
}

5.2 The DTO (TransferDto)

Annotated with JsonApi markers to define the structure (id, type).
Java

package com.bank.transfers.adapter.web.dto;

import com.toedter.spring.hateoas.jsonapi.JsonApiId;
import com.toedter.spring.hateoas.jsonapi.JsonApiTypeForClass;
import org.springframework.hateoas.RepresentationModel;
import java.math.BigDecimal;

@JsonApiTypeForClass("transfers")
public class TransferDto extends RepresentationModel<TransferDto> {

    @JsonApiId
    private String id;
    private BigDecimal amount;
    private String currency;
    private String status;

    public TransferDto() {} // Required by Jackson

    public TransferDto(String id, BigDecimal amount, String currency, String status) {
        this.id = id;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }
    
    // Getters and Setters omitted for brevity, but MUST be present.
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

5.3 The Mapper (TransferMapper)

Uses MapStruct to cleanly separate Domain objects from DTOs.
Java

package com.bank.transfers.adapter.web.mapper;

import com.bank.transfers.adapter.web.dto.TransferDto;
import com.bank.transfers.domain.BookTransfer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TransferMapper {

    TransferDto toDto(BookTransfer domain);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "PENDING")
    BookTransfer toDomain(TransferDto dto);
}

5.4 The Assembler (TransferAssembler)

This class is crucial. It handles the conversion of Page<Domain> to PagedModel<EntityModel<Dto>>. It uses ServletUriComponentsBuilder to ensure that next/prev links automatically retain all filter and sort parameters.
Java

package com.bank.transfers.adapter.web;

import com.bank.transfers.adapter.web.dto.TransferDto;
import com.bank.transfers.adapter.web.mapper.TransferMapper;
import com.bank.transfers.domain.BookTransfer;
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
public class TransferAssembler extends RepresentationModelAssemblerSupport<BookTransfer, EntityModel<TransferDto>> {

    private final TransferMapper mapper;
    private final PagedResourcesAssembler<BookTransfer> pagedAssembler;

    public TransferAssembler(TransferMapper mapper, PagedResourcesAssembler<BookTransfer> pagedAssembler) {
        super(TransferController.class, (Class<EntityModel<TransferDto>>)(Class<?>)EntityModel.class);
        this.mapper = mapper;
        this.pagedAssembler = pagedAssembler;
    }

    @Override
    public EntityModel<TransferDto> toModel(BookTransfer entity) {
        TransferDto dto = mapper.toDto(entity);

        return EntityModel.of(dto,
            linkTo(methodOn(TransferController.class).getOne(entity.id())).withSelfRel()
        );
    }

    public PagedModel<EntityModel<TransferDto>> toPagedModel(Page<BookTransfer> page) {
        if (page.isEmpty()) {
            return (PagedModel<EntityModel<TransferDto>>) pagedAssembler.toEmptyModel(page, TransferDto.class);
        }

        // Creates valid Next/Prev links preserving current request params
        return pagedAssembler.toModel(page, this,
            ServletUriComponentsBuilder.fromCurrentRequest()::build
        );
    }
}

5.5 The Controller (TransferController)

The final Input Adapter. It is clean, readable, and devoid of parsing logic.
Java

package com.bank.transfers.adapter.web;

import com.bank.infra.jsonapi.DomainQuery;
import com.bank.infra.jsonapi.JsonApiQuery;
import com.bank.infra.jsonapi.JsonApiController;
import com.bank.transfers.adapter.web.dto.TransferDto;
import com.bank.transfers.adapter.web.mapper.TransferMapper;
import com.bank.transfers.domain.BookTransfer;
import com.bank.transfers.domain.TransferUseCase;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@JsonApiController
@RequestMapping(path = "/api/v1/transfers", produces = "application/vnd.api+json")
public class TransferController {

    private final TransferUseCase useCase;
    private final TransferAssembler assembler;
    private final TransferMapper mapper;

    public TransferController(TransferUseCase useCase, TransferAssembler assembler, TransferMapper mapper) {
        this.useCase = useCase;
        this.assembler = assembler;
        this.mapper = mapper;
    }

    /**
     * Search with Filtering and Pagination.
     * Example: GET /api/v1/transfers?filter[amount][gt]=100&page[number]=1
     */
    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<TransferDto>>> search(
            @JsonApiQuery DomainQuery query) {
        
        Page<BookTransfer> results = useCase.search(query);
        return ResponseEntity.ok(assembler.toPagedModel(results));
    }

    /**
     * Create Resource.
     * Example: POST /api/v1/transfers { "data": { ... } }
     */
    @PostMapping(consumes = "application/vnd.api+json")
    public ResponseEntity<EntityModel<TransferDto>> create(
            @RequestBody EntityModel<TransferDto> input) {
        
        // 1. Unwrap
        TransferDto dto = input.getContent();
        if (dto == null) throw new IllegalArgumentException("Attributes missing");

        // 2. Map & Execute
        BookTransfer command = mapper.toDomain(dto);
        BookTransfer created = useCase.create(command);

        // 3. Wrap & Return
        return ResponseEntity.status(201).body(assembler.toModel(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<TransferDto>> getOne(@PathVariable String id) {
        BookTransfer result = useCase.get(id);
        return ResponseEntity.ok(assembler.toModel(result));
    }
}

6. Conclusion

This blueprint provides a robust foundation for complying with the JSON:API specification. By delegating Parsing to the Resolver, Mapping to MapStruct, and Formatting to Spring HATEOAS and json-api-lib, the implementation remains highly maintainable and true to Hexagonal principles.