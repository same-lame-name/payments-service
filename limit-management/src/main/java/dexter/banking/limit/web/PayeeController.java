package dexter.banking.limit.web;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.pipeline.core.PipelineOrchestrator;
import dexter.banking.limit.service.PayeeQueryService;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@JsonApiController
@RequestMapping(path = "/api/v1/payees", produces = "application/vnd.api+json")
public class PayeeController {

    private final PayeeQueryService service;
    private final PayeeAssembler assembler;
    private final PipelineOrchestrator<PayeeDto, PayeeDto> orchestrator;

    public PayeeController(PayeeQueryService service,
                           PayeeAssembler assembler,
                           PipelineOrchestrator<PayeeDto, PayeeDto> orchestrator) {
        this.service = service;
        this.assembler = assembler;
        this.orchestrator = orchestrator;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> list(
            @JsonApiFilter Node filter,
            @JsonApiSort Sort sort,
            @JsonApiPage Pageable pageable) {

        Page<PayeeDto> payees = service.list(filter, sort, pageable);
        return ResponseEntity.ok(assembler.toPagedModel(payees));
    }

    @GetMapping("/online")
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> onlineList(
            @JsonApiFilter Node filter,
            @JsonApiSort Sort sort,
            @JsonApiPage Pageable pageable) {

        Page<PayeeDto> payees = service.listOnline(filter, sort, pageable);
        return ResponseEntity.ok(assembler.toPagedModel(payees));
    }

    @PostMapping
    public ResponseEntity<EntityModel<PayeeDto>> create(@RequestBody EntityModel<PayeeDto> requestBody) {
        PayeeDto dto = requestBody.getContent();
        PayeeDto savedPayeeDto = orchestrator.handle(dto);
        
        EntityModel<PayeeDto> model = assembler.toModel(savedPayeeDto);
        
        URI location = model.getLink("self")
                .map(link -> URI.create(link.getHref()))
                .orElse(URI.create("/api/v1/payees/" + savedPayeeDto.getId()));

        return ResponseEntity.created(location).body(model);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PayeeDto>> getOne(@PathVariable String id) {
        return service.getOne(id)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}