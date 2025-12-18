Here is the Unambiguous Architectural Directive for your Agent.
Architectural Directive: Payee Update Feature (RFC 7396)
1. Feature Specifications

We are implementing the Update Payee feature using RFC 7396 (JSON Merge Patch) semantics.

    Behavior:

        Field Present & Value: Update.

        Field Present & Null: Delete/Clear.

        Field Missing: Ignore.

    Validation: Strict Mode. Clients must not send instructions (Value or Null) for fields that are not configured as "Editable" in the current market.

    Service Model: Dual-Merge Pattern. The Service operates on a "Sane" Business Object for logic, avoiding wrapper complexity in the core domain.

2. Data Structure Definition
   A. The Input Patch (Wrapper DTO)

Goal: Capture the partial intent using JsonNullable.

    Dependency: jackson-databind-nullable (or internal implementation).

    Class: UpdatePayeePatch extends BasePaymentRequest.

    Fields: All updatable fields must be wrapped in JsonNullable<T>.

B. The Business Object (Sane DTO)

Goal: A complete, valid representation of the Payee for business logic.

    Class: PayeeBusinessDto.

    Fields: Standard Java types (String, BigDecimal). No Wrappers.

C. The Persistence Object

Goal: Database state.

    Class: PayeeEntity.

3. Implementation Instructions
   Step 1: The Strict Validator (Pipeline)

Logic: Fail fast if the user includes any field (Value or Null) that is not in the Allowlist.
Java

@Component
@Order(50) // Business Rules Step
public class StrictMergePatchValidator implements PaymentPipelineStep {

    @Override
    public void process(BasePaymentRequest request) {
        if (!(request instanceof UpdatePayeePatch patch)) return;
        
        Set<String> allowed = request.getRulesConfig().getEditableFields();
        
        // Strict Check: If it is PRESENT, it must be ALLOWED.
        // We do not care if the value is null or equals existing.
        // The intent itself is forbidden.
        if (patch.getNickname().isPresent()) validate("nickname", allowed);
        if (patch.getEmail().isPresent()) validate("email", allowed);
        if (patch.getNationality().isPresent()) validate("nationality", allowed);
    }
    
    private void validate(String field, Set<String> allowed) {
        if (!allowed.contains(field)) {
            throw new BusinessRuleException("FIELD_LOCKED", "Field " + field + " is not editable.");
        }
    }
}

Step 2: The Mapper (MapStruct)

Constraint: Must use NullValueCheckStrategy.ALWAYS so MapStruct generates if (patch.field.isPresent()) checks.
Java

@Mapper(
componentModel = "spring",
nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
uses = JsonNullableMapper.class // If needed for unwrapping
)
public interface PayeeMapper {

    // 1. Hydration
    PayeeBusinessDto toDomain(PayeeEntity entity);

    // 2. Business Merge (Patch -> Sane DTO)
    void updateDomainFromPatch(UpdatePayeePatch patch, @MappingTarget PayeeBusinessDto target);

    // 3. Persistence Merge (Patch -> Entity)
    void updateEntityFromPatch(UpdatePayeePatch patch, @MappingTarget PayeeEntity target);
}

Step 3: The Service Strategy (Dual-Merge)

Constraint: Never operate on the Entity for business logic. Hydrate the Business DTO first.
Java

@Service
@RequiredArgsConstructor
public class UpdatePayeeStrategy implements PaymentServiceStrategy {

    private final PayeeRepository repo;
    private final PayeeMapper mapper;
    private final GatewayClient gateway; // Example dependency

    @Override
    @Transactional
    public PaymentResponse execute(BasePaymentRequest req) {
        UpdatePayeePatch patch = (UpdatePayeePatch) req;

        // 1. Load Persistence State
        PayeeEntity entity = repo.findById(patch.getPayeeId())
            .orElseThrow(() -> new NotFoundException("Payee not found"));

        // 2. Hydrate & Merge to Business State (Sane Object)
        // This ensures 'businessObject' is the complete final state (Existing + Changes)
        PayeeBusinessDto businessObject = mapper.toDomain(entity);
        mapper.updateDomainFromPatch(patch, businessObject);

        // --- SANE ZONE START ---
        
        // 3. Business Logic on Unwrapped Object
        // Example: Gateway sync requires full state
        gateway.syncPayee(businessObject);

        // Example: Cross-field validation
        if (businessObject.getEmail() == null && businessObject.getMobile() == null) {
            throw new BusinessException("Payee must have at least one contact method.");
        }

        // --- SANE ZONE END ---

        // 4. Persistence Merge & Save
        mapper.updateEntityFromPatch(patch, entity);
        repo.save(entity);
        
        // 5. Audit (Handled by Listener via DTO Snapshot/Event)
        // Note: The patch itself is the event payload
        
        return new PaymentResponse("SUCCESS", businessObject); // Return the final state
    }
}

4. Constraints Checklist

   Boilerplate: Do not write manual if (isPresent) checks in the Service. Rely on MapStruct.

   Safety: Always hydrate toDomain(entity) before merging. Never create a fresh DTO from the patch.

   Strictness: Pipeline validation must run before the Service. Service assumes valid inputs.