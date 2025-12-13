package dexter.banking.limit.web;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.config.JsonApiConstants;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.service.PayeeService;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@JsonApiController
@RequestMapping(path = "/api/v1/payees", produces = JsonApiConstants.MEDIA_TYPE)
public class PayeeController {

    private final PayeeService service;
    private final PayeeAssembler assembler;
    private final PagedResourcesAssembler<Payee> pagedResourcesAssembler;

    public PayeeController(PayeeService service,
                           PayeeAssembler assembler,
                           PagedResourcesAssembler<Payee> pagedResourcesAssembler) {
        this.service = service;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> list(
            @JsonApiFilter Node filter,
            @JsonApiSort Sort sort,
            @JsonApiPage Pageable pageable) {

        Page<Payee> payees = service.list(filter, sort, pageable);
        PagedModel<EntityModel<PayeeDto>> pagedModel = pagedResourcesAssembler.toModel(payees, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/online")
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> onlineList(
            @JsonApiFilter Node filter,
            @JsonApiSort Sort sort,
            @JsonApiPage Pageable pageable) {

        Page<Payee> payees = service.listOnline(filter, sort, pageable);
        PagedModel<EntityModel<PayeeDto>> pagedModel = pagedResourcesAssembler.toModel(payees, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @PostMapping
    public ResponseEntity<EntityModel<PayeeDto>> create(@RequestBody EntityModel<PayeeDto> requestBody) {
        PayeeDto dto = requestBody.getContent();
        Payee domainToCreate = assembler.toDomain(dto);
        
        Payee savedPayee = service.create(domainToCreate);
        
        return ResponseEntity.created(URI.create("/api/v1/payees/" + savedPayee.getId()))
                .body(assembler.toModel(savedPayee));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PayeeDto>> getOne(@PathVariable String id) {
        return service.getOne(id)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}