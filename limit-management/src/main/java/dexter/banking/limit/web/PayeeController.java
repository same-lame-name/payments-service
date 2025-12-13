package dexter.banking.limit.web;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.gateway.RegulatorGateway;
import dexter.banking.limit.repository.PayeeRepository;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    public PayeeController(PayeeRepository repository,
                           PayeeAssembler assembler,
                           PagedResourcesAssembler<Payee> pagedResourcesAssembler,
                           RegulatorGateway regulatorGateway) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.regulatorGateway = regulatorGateway;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> list(
            @JsonApiQuery DomainQuery query,
            @RequestParam(name = "enrich", defaultValue = "false") boolean enrich) {

        Page<Payee> payees;
        if (enrich) {
            payees = getEnrichedPayees(query.pageable());
        } else {
            payees = repository.findAll(query);
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
        return ResponseEntity.notFound().build();
    }
}