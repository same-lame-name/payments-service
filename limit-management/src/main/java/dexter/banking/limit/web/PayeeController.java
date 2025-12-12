package dexter.banking.limit.web;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.repository.PayeeRepository;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@JsonApiController
@RequestMapping(path = "/api/v1/payees", produces = "application/vnd.api+json")
public class PayeeController {

    private final PayeeRepository repository;
    private final PayeeAssembler assembler;
    private final PagedResourcesAssembler<Payee> pagedResourcesAssembler;

    public PayeeController(PayeeRepository repository, 
                           PayeeAssembler assembler, 
                           PagedResourcesAssembler<Payee> pagedResourcesAssembler) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> list(@JsonApiQuery DomainQuery query) {
        Page<Payee> payees = repository.findAll(query);
        
        // Converts Page<Domain> -> PagedModel<Resource> with Links (first, prev, next, last)
        PagedModel<EntityModel<PayeeDto>> pagedModel = pagedResourcesAssembler.toModel(payees, assembler);
        
        return ResponseEntity.ok(pagedModel);
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