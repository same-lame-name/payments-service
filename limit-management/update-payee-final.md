Architectural Directive: Banking Core Payment System
1. Project Context & Motivation

We are building a highly scalable, configuration-driven banking payment engine designed to serve 24 Markets with ~15 Payment Journeys per market.

Core Architectural Pillars:

    Pipeline-First Processing: All pre-processing (Config, Validation, Enrichment) happens in a standardized, ordered pipeline before business logic executes.

    Strict Strategy Routing: Execution logic is selected via a Registry Map (O(1)) that enforces uniqueness and safety at startup.

    Rich Domain Model: The Request DTO acts as the Aggregate Root, managing its own state transitions and event registration.

    Shadow Table Auditing: Auditing is an asynchronous, non-blocking side-effect that persists deep snapshots into a mirrored SQL table (PAYMENTS_AUDIT).

    RFC 7396 Patching: Partial updates follow strict Merge Patch semantics (null=Delete, Missing=Ignore) with strict allowlist validation.

2. High-Level Flow

   Ingestion: Controller (HATEOAS compliant) receives Request -> Delegates to PipelineOrchestrator.

   Pre-Processing: Orchestrator runs ordered PipelineMiddleware (Config Load -> Validate -> Enrich -> Rules).

   Routing: Orchestrator resolves the JourneyKey and fetches the PaymentServiceStrategy.

   Execution: Service performs atomic business logic using "Sane" Unwrapped DTOs.

   Auditing: Async Listener persists snapshots to the Shadow Table.

3. Core Component Specifications
   3.1. The Pre-Processing Pipeline

Goal: Decouple "Housekeeping" from "Core Logic".

    Interface: PipelineMiddleware extending Ordered.

    Orchestrator: PipelineOrchestrator. Injects List<PipelineMiddleware>. Iterates sequentially.

    Failure Mode: Fail-Fast on first exception.

3.2. The Execution Strategy (Router)

Goal: Safe, high-performance selection of business logic.

    Interface: PaymentServiceStrategy.

        Method: Set<String> getSupportedIdentifiers().

        Method: execute(BaseRequest request).

    Registry: PaymentServiceRegistry.

        Startup: Scans strategies, builds Map<String, Strategy>, throws Exception on key collision.

3.3. Auditing Subsystem (Shadow Table)

Goal: Detailed SQL-based audit trail without impacting SLA.

    Storage: Relational Table PAYMENTS_AUDIT.

    Deep Cloning: DTOs must implement createSnapshot() using MapStruct (Static Instance). No Copy Constructors.

    Listener: @Async("auditExecutor") + @TransactionalEventListener(phase = AFTER_COMMIT).

    Mapping: Use Spring-managed AuditMapper to convert Snapshot DTO -> Audit Entity.

4. Feature Specification: Update Payee (RFC 7396)
   4.1. Data Structures

A. Input Patch (Wrapper DTO):

    Class: UpdatePayeePatch extends BasePaymentRequest.

    Fields: Use JsonNullable<T> for all updatable fields. Mix of Raw (Metadata) and Wrapped (Payload) fields is allowed.

    Annotations: Respect @JsonApiTypeForClass and HATEOAS standards.

B. Business Object (Sane DTO):

    Class: PayeeDto.

    Fields: Standard Java types. No Wrappers.

C. Persistence Object:

    Class: PayeeEntity.

4.2. Validation (Strict Reflection)

Goal: Fail fast if client sends a field (Value or Null) not in the Market's "Editable" allowlist.

    Mechanism: Use Reflection (cached fields) to iterate over UpdatePayeePatch.

    Logic:

        Is field JsonNullable?

        Is it isPresent()?

        If Yes -> Is field name in Allowlist?

        If No -> Throw BusinessRuleException.

4.3. Service Strategy (Dual-Merge Pattern)

Goal: Operate on Sane DTOs for logic, Entity for persistence.

    Load: Fetch PayeeEntity.

    Hydrate: Map Entity -> PayeeDto (Sane Base State).

    Merge 1: Map UpdatePayeePatch -> PayeeDto. (Result: Final Business State).

    Logic: Run validations/Gateway calls on PayeeDto.

    Merge 2: Map UpdatePayeePatch -> PayeeEntity.

    Persist: Save Entity.

5. Code Scaffolding Instructions
   A. The Patch DTO (Input)
   Java

@Data
@JsonApiTypeForClass("payees")
public class UpdatePayeePatch extends BasePaymentRequest<UpdatePayeePatch> {
@JsonApiId
private String id;

    // Payload Fields (Wrapped)
    private JsonNullable<String> name = JsonNullable.undefined();
    private JsonNullable<String> email = JsonNullable.undefined();
    private JsonNullable<String> nationality = JsonNullable.undefined();

    @Override
    public String getJourneyIdentifier() { return "PAYEE-UPDATE"; }
}

B. The Strict Validator (Cached Reflection)
Java

@Component
@Order(50)
public class StrictMergePatchValidator implements PipelineMiddleware<BaseRequest> {

    private static final List<Field> PATCH_FIELDS;
    static {
        List<Field> f = new ArrayList<>();
        ReflectionUtils.doWithFields(UpdatePayeePatch.class, field -> {
            if (JsonNullable.class.isAssignableFrom(field.getType())) {
                field.setAccessible(true);
                f.add(field);
            }
        });
        PATCH_FIELDS = Collections.unmodifiableList(f);
    }

    @Override
    public void process(BaseRequest request) {
        if (!(request instanceof UpdatePayeePatch patch)) return;
        
        Set<String> allowed = request.getRulesConfig().getEditableFields();
        
        for (Field field : PATCH_FIELDS) {
            try {
                JsonNullable<?> wrapper = (JsonNullable<?>) field.get(patch);
                if (wrapper != null && wrapper.isPresent()) {
                    if (!allowed.contains(field.getName())) {
                         throw new BusinessRuleException("FIELD_LOCKED", "Field '" + field.getName() + "' is not editable.");
                    }
                }
            } catch (IllegalAccessException e) { /* Impossible due to static block */ }
        }
    }
}

C. The Controller (PATCH Endpoint)
Java

@PatchMapping("/{id}")
public ResponseEntity<EntityModel<PayeeDto>> update(
@PathVariable String id,
@RequestBody EntityModel<UpdatePayeePatch> body,
@RequestHeader(value = "Idempotency-Key", required = false) String key) {

    UpdatePayeePatch patch = body.getContent();
    patch.setId(id);
    patch.setIdempotencyKey(key);

    // Returns the Sane PayeeDto (Final State)
    PayeeDto result = orchestrator.handle(patch);
    
    return ResponseEntity.ok(assembler.toModel(result));
}

D. The Mapper (MapStruct)
Java

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PayeeMapper {
// 1. Hydrate
PayeeDto toDto(PayeeEntity entity);

    // 2. Business Merge (Patch -> Sane DTO)
    void updateDtoFromPatch(UpdatePayeePatch patch, @MappingTarget PayeeDto dto);

    // 3. Persistence Merge (Patch -> Entity)
    void updateEntityFromPatch(UpdatePayeePatch patch, @MappingTarget PayeeEntity entity);
}

E. The Service (Dual-Merge)
Java

@Service
@RequiredArgsConstructor
public class UpdatePayeeStrategy implements PaymentServiceStrategy {
private final PayeeRepository repo;
private final PayeeMapper mapper;

    @Override
    @Transactional
    public PayeeDto execute(BaseRequest req) {
        UpdatePayeePatch patch = (UpdatePayeePatch) req;

        // 1. Load
        PayeeEntity entity = repo.findById(patch.getId()).orElseThrow();

        // 2. Hydrate & Merge (Business State)
        PayeeDto businessDto = mapper.toDto(entity);
        mapper.updateDtoFromPatch(patch, businessDto);

        // 3. Logic (Sane Zone)
        // e.g. gateway.sync(businessDto);

        // 4. Persist
        mapper.updateEntityFromPatch(patch, entity);
        repo.save(entity);

        return businessDto;
    }
}