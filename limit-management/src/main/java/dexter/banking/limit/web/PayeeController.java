package dexter.banking.limit.web;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.pipeline.core.PipelineOrchestrator;
import dexter.banking.limit.service.PayeeQueryService;
import dexter.banking.limit.web.dto.PayeeDto;
import dexter.banking.limit.web.dto.UpdatePayeePatch;
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
    private final PipelineOrchestrator<UpdatePayeePatch, PayeeDto> updateOrchestrator;

    public PayeeController(PayeeQueryService service,
                           PayeeAssembler assembler,
                           PipelineOrchestrator<PayeeDto, PayeeDto> orchestrator,
                           PipelineOrchestrator<UpdatePayeePatch, PayeeDto> updateOrchestrator) {
        this.service = service;
        this.assembler = assembler;
        this.orchestrator = orchestrator;
        this.updateOrchestrator = updateOrchestrator;
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
    public ResponseEntity<EntityModel<PayeeDto>> create(
            @RequestBody EntityModel<PayeeDto> requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        PayeeDto dto = requestBody.getContent();
        dto.setIdempotencyKey(idempotencyKey);
        PayeeDto savedPayeeDto = orchestrator.handle(dto);

        EntityModel<PayeeDto> model = assembler.toModel(savedPayeeDto);
        
        URI location = model.getLink("self")
                .map(link -> URI.create(link.getHref()))
                .orElse(URI.create("/api/v1/payees/" + savedPayeeDto.getId()));

        return ResponseEntity.created(location).body(model);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<EntityModel<PayeeDto>> update(
            @PathVariable String id,
            @RequestBody EntityModel<UpdatePayeePatch> body,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {

        UpdatePayeePatch patch = body.getContent();
        patch.setId(id);
        patch.setIdempotencyKey(idempotencyKey);

        PayeeDto result = updateOrchestrator.handle(patch);

        return ResponseEntity.ok(assembler.toModel(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PayeeDto>> getOne(@PathVariable String id) {
        return service.getOne(id)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}