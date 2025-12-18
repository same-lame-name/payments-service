Architectural Directive: Domain Event Publishing & Auditing Subsystem
1. Subsystem Context & Motivation

We are implementing a Rich Domain Model combined with a Shadow Table Audit Pattern.

    Current State: The Request Pipeline is implemented.

    Target State: We need to implement the State Transition logic, Event Publishing, and Asynchronous Auditing.

    Core Philosophy:

        Encapsulation: The DTO (BasePaymentRequest) is the Aggregate Root. It manages its own state transitions and registers events internally.

        Audit Fidelity: Audit logs rely on a Deep Clone Snapshot of the DTO at the exact moment of transition.

        Performance: Auditing is an asynchronous, non-blocking side-effect.

        Consistency: Audit data is mapped to a strictly typed SQL Shadow Table (PAYMENTS_AUDIT), not dumped as JSON.

2. Component Design Specifications
   2.1. Deep Cloning Strategy (The Memento)

Goal: Create a detached, deep copy of the Request DTO to serve as an immutable snapshot for the Event. Constraint: Use MapStruct (Static Instance). Do not use Copy Constructors (boilerplate) or Lombok (shallow copy risk).

    Component: PaymentDeepCloner (MapStruct Mapper).

    Configuration: mappingControl = DeepClone.class (or manual deep copy config).

    Access: PaymentDeepCloner.INSTANCE (No Spring dependency).

2.2. The Rich Domain Model (Aggregate Root)

Goal: Manage state transitions and strictly control event registration.

    Base Class: DomainEventAware.

        Maintains a List<DomainEvent>.

        Exposes releaseEvents(): Returns and clears the list.

    Aggregate: BasePaymentRequest.

        Abstract Method: createSnapshot() (Must be implemented by subclasses to call the specific MapStruct cloner).

        State Methods: markDebited(), markCredited(). These methods:

            Validate the transition.

            Update the internal status field.

            Call createSnapshot() to get a safe copy.

            Register a PaymentStatusChangedEvent containing the snapshot.

        Process Events: Public API addProcessEvent() for Service-driven events (e.g., Risk Check).

2.3. The Domain Event

Goal: Carry the state of the world.

    Class: PaymentStatusChangedEvent.

    Payload: Contains BasePaymentRequest payloadSnapshot (The deep clone).

2.4. Infrastructure: The Audit Listener

Goal: Persist the audit trail without blocking the main transaction.

    Trigger: @TransactionalEventListener(phase = AFTER_COMMIT).

        Why: We only audit transactions that successfully committed to the PAYMENTS table.

    Concurrency: @Async("auditExecutor").

        Why: Failure or latency in the Audit DB must not impact the Customer response time.

    Mapping: Uses AuditMapper (Spring Component).

        Why: To map the Snapshot DTO -> PaymentAuditEntity (Shadow Table).

    Persistence: AuditRepository saves to PAYMENTS_AUDIT.

3. Code Scaffolding Instructions

The Agent must generate the following components adhering strictly to these patterns:
A. The Deep Cloner (MapStruct - Static)
Java

@Mapper
public interface PaymentDeepCloner {
// 1. Singleton Instance for DTO usage
PaymentDeepCloner INSTANCE = Mappers.getMapper(PaymentDeepCloner.class);

    // 2. Deep Clone Logic (Recursive if needed)
    DomesticTransferRequest clone(DomesticTransferRequest original);
    InternationalTransferRequest clone(InternationalTransferRequest original);
    
    // Ensure nested objects (CustomerProfile) are also cloned, not referenced
    CustomerProfile clone(CustomerProfile original);
}

B. The Domain Core (DTO & Events)
Java

// 1. The Base Event Manager
public abstract class DomainEventAware {
@JsonIgnore
private final List<DomainEvent> events = new ArrayList<>();

    protected void registerEvent(DomainEvent e) { events.add(e); }
    
    // Public API for Service-originated events (e.g. Sanctions Check)
    public void addProcessEvent(DomainEvent e) { registerEvent(e); }

    // "Drain" method
    public List<DomainEvent> releaseEvents() {
        List<DomainEvent> released = new ArrayList<>(events);
        events.clear();
        return released;
    }
}

// 2. The Aggregate Root
public abstract class BasePaymentRequest extends DomainEventAware {
// Abstract hook for cloning
public abstract BasePaymentRequest createSnapshot();

    // STRICT State Transition (Service calls this, not setStatus)
    public void markDebited(String bankReference) {
        if (this.status != PaymentStatus.INITIATED) throw new IllegalStateException(...);
        
        this.status = PaymentStatus.DEBITED;
        
        // Register Event with SNAPSHOT
        BasePaymentRequest snapshot = this.createSnapshot();
        this.registerEvent(new PaymentStatusChangedEvent(snapshot, PaymentStatus.INITIATED, PaymentStatus.DEBITED, ...));
    }
}

// 3. The Concrete Implementation
public class DomesticTransferRequest extends BasePaymentRequest {
@Override
public BasePaymentRequest createSnapshot() {
return PaymentDeepCloner.INSTANCE.clone(this);
}
}

C. The Service Layer (Orchestration)
Java

@Service
public class DomesticPaymentStrategy implements PaymentServiceStrategy {
@Transactional
public PaymentResponse execute(BasePaymentRequest request) {
// 1. Perform Business Logic
request.markDebited("REF123");

        // 2. Persist Main Entity
        repository.save(request);
        
        // 3. Release & Publish Events (Spring Event Bus)
        request.releaseEvents().forEach(publisher::publishEvent);
        
        return new PaymentResponse("SUCCESS");
    }
}

D. The Audit Infrastructure (Async Listener)
Java

@Component
@Slf4j
public class AuditEventListener {

    private final AuditMapper auditMapper; // Spring-managed MapStruct
    private final AuditRepository auditRepo;

    @Async("auditExecutor") // Non-blocking
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Only on success
    public void handle(PaymentStatusChangedEvent event) {
        try {
            // 1. Get Snapshot
            BasePaymentRequest snapshot = event.getPayloadSnapshot();
            
            // 2. Map Snapshot -> Shadow Entity
            PaymentAuditEntity entity = auditMapper.toEntity(snapshot);
            
            // 3. Enrich & Save
            entity.setTimestamp(event.getOccurredOn());
            auditRepo.save(entity);
            
        } catch (Exception e) {
            // Log failure but do NOT throw (Main Tx is already committed)
            log.error("AUDIT_FAIL: Payment {} committed but audit failed.", event.getPaymentId(), e);
        }
    }
}

4. Summary of Constraints

   Isolation: The Pipeline and Registry are out of scope for this task. Focus only on DTO/Event/Listener.

   No JSON: Audit data must be mapped to columns in the PAYMENTS_AUDIT table via AuditMapper.

   Cloning: Use PaymentDeepCloner.INSTANCE inside DTOs.

   Safety: Audits are AFTER_COMMIT and Async. Failures in auditing must be logged but must not rollback the main transaction