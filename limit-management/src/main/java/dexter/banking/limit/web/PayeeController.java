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
    public ResponseEntity<EntityModel<PayeeDto>> create(
            @RequestBody EntityModel<PayeeDto> requestBody,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        
        PayeeDto dto = requestBody.getContent();
        dto.setIdempotencyKey(idempotencyKey);
        PayeeDto savedPayeeDto = orchestrator.handle(dto);
        //When I post to the orchestrator.
        // 1. Runs through all the middlewares in order
        // 2. It uses the strategy pattern to select and send the rewquest to hte serivce.

        //Controller > Strategy pattern (On basis of request-type, we choose the service) Service
        // Service =>
        // 1. We need to copy the values from ThreadLocal (header) to DTO.
        // 2. Loads the service-config / rules-config
        // 3. Idempotency check ::
        //  a. If the idempotency is new :: move forward
        //  b. if the idempotency is old and encountered then I short circuit and return the cached value
        // 3. Syntactic validations (Ordered validations)
        // 4. Data-collectors :: May or may not :: this can be controlled using the service-config / rule-config
        // 5. Rule-engine runs (business rules) :: May or may not :: this can be controlled using the service-config / rule-config
        // 6. We start with actual processing.

        //Command requests :: instructing us to do something :: we want safe
        // query reqeuts :: inquiring from DB or something :: we want fast

        // CQRS (Command and Query Request Segragation)


        //Controller > pipeline > stage1 > stage2 > stage3 > stageN > service.
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