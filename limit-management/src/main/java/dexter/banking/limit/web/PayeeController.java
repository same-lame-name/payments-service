package dexter.banking.limit.web;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.service.PayeeService;
import dexter.banking.limit.web.dto.PayeeDto;
import dexter.banking.limit.web.link.LinkToggleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@JsonApiController
@RequestMapping(path = "/api/v1/payees", produces = "application/vnd.api+json")
public class PayeeController {

    private final PayeeService service;
    private final PayeeAssembler assembler;
    private final PagedResourcesAssembler<Payee> pagedResourcesAssembler;
    private final LinkToggleService linkToggleService;

    public PayeeController(PayeeService service,
                           PayeeAssembler assembler,
                           PagedResourcesAssembler<Payee> pagedResourcesAssembler,
                           LinkToggleService linkToggleService) {
        this.service = service;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.linkToggleService = linkToggleService;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> list(
            @JsonApiFilter Node filter,
            @JsonApiSort Sort sort,
            @JsonApiPage Pageable pageable) {

        Page<Payee> payees = service.list(filter, sort, pageable);
        
        PagedModel<EntityModel<PayeeDto>> pagedModel;
        if (linkToggleService.isLinksEnabled()) {
            pagedModel = pagedResourcesAssembler.toModel(payees, assembler);
        } else {
            List<EntityModel<PayeeDto>> content = payees.getContent().stream()
                    .map(assembler::toModel)
                    .collect(Collectors.toList());
            PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                    payees.getSize(), payees.getNumber(), payees.getTotalElements(), payees.getTotalPages());
            pagedModel = PagedModel.of(content, metadata);
        }

        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/online")
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> onlineList(
            @JsonApiFilter Node filter,
            @JsonApiSort Sort sort,
            @JsonApiPage Pageable pageable) {

        Page<Payee> payees = service.listOnline(filter, sort, pageable);
        
        PagedModel<EntityModel<PayeeDto>> pagedModel;
        if (linkToggleService.isLinksEnabled()) {
            pagedModel = pagedResourcesAssembler.toModel(payees, assembler);
        } else {
            List<EntityModel<PayeeDto>> content = payees.getContent().stream()
                    .map(assembler::toModel)
                    .collect(Collectors.toList());
            PagedModel.PageMetadata metadata = new PagedModel.PageMetadata(
                    payees.getSize(), payees.getNumber(), payees.getTotalElements(), payees.getTotalPages());
            pagedModel = PagedModel.of(content, metadata);
        }

        return ResponseEntity.ok(pagedModel);
    }

    @PostMapping
    public ResponseEntity<EntityModel<PayeeDto>> create(@RequestBody EntityModel<PayeeDto> requestBody) {
        PayeeDto dto = requestBody.getContent();
        Payee domainToCreate = assembler.toDomain(dto);
        
        Payee savedPayee = service.create(domainToCreate);
        
        EntityModel<PayeeDto> model = assembler.toModel(savedPayee);
        
        if (linkToggleService.isLinksEnabled()) {
            return ResponseEntity.created(URI.create(model.getRequiredLink("self").getHref()))
                    .body(model);
        } else {
            return ResponseEntity.created(URI.create("/api/v1/payees/" + savedPayee.getId()))
                    .body(model);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PayeeDto>> getOne(@PathVariable String id) {
        return service.getOne(id)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}