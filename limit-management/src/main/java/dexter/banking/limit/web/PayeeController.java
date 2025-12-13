package dexter.banking.limit.web;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.gateway.RegulatorGateway;
import dexter.banking.limit.repository.PayeeRepository;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import dexter.banking.limit.repository.rsql.jpa.JpaRsqlVisitor;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@JsonApiController
@RequestMapping(path = "/api/v1/payees", produces = "application/vnd.api+json")
public class PayeeController {

    private final PayeeRepository repository;
    private final PayeeAssembler assembler;
    private final PagedResourcesAssembler<Payee> pagedResourcesAssembler;
    private final RegulatorGateway regulatorGateway;
    private final FilterConfig<String> jpaFilterConfig;

    public PayeeController(PayeeRepository repository,
                           PayeeAssembler assembler,
                           PagedResourcesAssembler<Payee> pagedResourcesAssembler,
                           RegulatorGateway regulatorGateway,
                           @Qualifier("payeeJpaFilterConfig") FilterConfig<String> jpaFilterConfig) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.regulatorGateway = regulatorGateway;
        this.jpaFilterConfig = jpaFilterConfig;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> list(
            @JsonApiFilter Node filter,
            @JsonApiSort Sort sort,
            @JsonApiPage Pageable pageable,
            @RequestParam(name = "enrich", defaultValue = "false") boolean enrich) {

        Page<Payee> payees;
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        if (enrich) {
            payees = getEnrichedPayees(pageRequest);
        } else {
            Specification<Payee> spec = (filter != null) ? filter.accept(new JpaRsqlVisitor<>(jpaFilterConfig)) : null;
            payees = repository.findAll(spec, pageRequest);
        }

        PagedModel<EntityModel<PayeeDto>> pagedModel = pagedResourcesAssembler.toModel(payees, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    private Page<Payee> getEnrichedPayees(Pageable pageable) {
        // 1. Fetch data from both sources
        List<Payee> localPayees = repository.findAll();
        List<Payee> regulatorPayees = regulatorGateway.fetchPayees();

        // 2. Merge them
        List<Payee> allPayees = new ArrayList<>(localPayees);
        allPayees.addAll(regulatorPayees);

        // 3. Apply in-memory pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allPayees.size());

        if (start > allPayees.size()) {
            return new PageImpl<>(List.of(), pageable, allPayees.size());
        }

        List<Payee> pageContent = allPayees.subList(start, end);
        return new PageImpl<>(pageContent, pageable, allPayees.size());
    }

    @PostMapping
    public ResponseEntity<EntityModel<PayeeDto>> create(@RequestBody EntityModel<PayeeDto> requestBody) {
        PayeeDto dto = requestBody.getContent();
        Payee domain = assembler.toDomain(dto);
        
        Payee saved = repository.save(domain);
        
        return ResponseEntity.created(URI.create("/api/v1/payees/" + saved.getId()))
                .body(assembler.toModel(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PayeeDto>> getOne(@PathVariable String id) {
        return repository.findById(id)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}