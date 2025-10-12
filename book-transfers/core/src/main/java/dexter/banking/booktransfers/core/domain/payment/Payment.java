package dexter.banking.booktransfers.core.domain.payment;


import dexter.banking.booktransfers.core.domain.payment.event.ManualInterventionRequiredEvent;
import dexter.banking.booktransfers.core.domain.payment.event.PaymentFailedEvent;
import dexter.banking.booktransfers.core.domain.payment.event.PaymentInProgressEvent;
import dexter.banking.booktransfers.core.domain.payment.event.PaymentRequiresComplianceCheck;
import dexter.banking.booktransfers.core.domain.payment.event.PaymentSuccessfulEvent;
import dexter.banking.booktransfers.core.domain.payment.valueobject.CreditLegResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.DebitLegResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.LimitEarmarkResult;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessAction;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.shared.policy.PolicyEvaluationContext;
import dexter.banking.booktransfers.core.domain.shared.primitives.AggregateRoot;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * Represents the central business entity in this domain: the Payment.
 *
 * <p><b>Architectural Role:</b> This class is the <b>Aggregate Root</b> for the Payment aggregate in a Domain-Driven
 * Design (DDD) context. An aggregate is a cluster of associated objects that are treated as a single unit for the
 * purpose of data changes. As the root, this class is the sole entry point for any command that modifies the state
 * of the payment. All business invariants for a payment are enforced within this boundary.
 *
 * <p><b>Design Patterns & Principles:</b>
 * <ul>
 *     <li><b>Encapsulation & State Transitions:</b> The aggregate's state is fully encapsulated. All modifications
 *         are performed exclusively through its own business methods (e.g., {@code recordDebit}, {@code recordCredit}).
 *         Each method represents a valid, atomic state transition, ensuring the aggregate is always in a consistent state.</li>
 *
 *     <li><b>Policy Pattern (Strategy):</b> The aggregate's state transition logic is decoupled from the business
 *         rules that govern it. The {@code transient BusinessPolicy policy} field holds a strategy object.
 *         Before any state change, the aggregate consults this policy via {@code policy.evaluate(...)}.
 *         This makes the core domain model stable while allowing complex business rules to be defined and
 *         modified externally in the infrastructure layer.</li>
 *
 *     <li><b>Memento Pattern:</b> The aggregate's internal state can be externalized into a {@link PaymentMemento}
 *         object via the {@code getMemento()} method. This allows for persistence (e.g., saving to a database)
 *         without breaking encapsulation. The {@code rehydrate()} static factory method restores an aggregate
 *         from a memento, cleanly separating domain logic from persistence concerns.</li>
 *
 *     <li><b>Domain Events:</b> The aggregate raises {@link dexter.banking.booktransfers.core.domain.shared.primitives.DomainEvent}s
 *         (e.g., {@link PaymentSuccessfulEvent}) upon successful state transitions by calling {@code registerEvent()}.
 *         This allows other parts of the system to react to changes without being tightly coupled, forming the
 *         basis of an event-driven architecture.</li>
 * </ul>
 */
@Getter
public class Payment extends AggregateRoot<UUID> {

    private final String transactionReference;
    private final String journeyName;
    private final transient BusinessPolicy policy;

    private DebitLegResult debitLegResult;
    private LimitEarmarkResult limitEarmarkResult;
    private CreditLegResult creditLegResult;
    private Status status;
    private PaymentState state;
    private Payment(UUID id, String transactionReference, String journeyName, BusinessPolicy policy, PaymentState state) {
        super(id);
        this.transactionReference = transactionReference;
        this.journeyName = journeyName;
        this.policy = policy;
        this.setState(state);
    }

    public static Payment startNew(PaymentCreationParams params, BusinessPolicy policy) {
        var payment = new Payment(params.transactionId(), params.transactionReference(), params.journeyName(), policy, PaymentState.NEW);
        PolicyEvaluationContext context = new PolicyEvaluationContext(payment.getMemento(), null);
        policy.evaluate(context, BusinessAction.START_PAYMENT);
        return payment;
    }

    public static Payment rehydrate(PaymentMemento memento, BusinessPolicy policy) {
        var payment = new Payment(memento.id(), memento.transactionReference(), memento.journeyName(), policy, memento.state());
        payment.debitLegResult = memento.debitLegResult();
        payment.limitEarmarkResult = memento.limitEarmarkResult();
        payment.creditLegResult = memento.creditLegResult();
        return payment;
    }

    public PaymentMemento getMemento() {
        return new PaymentMemento(
            this.id,
            this.transactionReference,
            this.journeyName,
            this.debitLegResult,
            this.limitEarmarkResult,
            this.creditLegResult,
            this.status,
            this.state
        );
    }

    // --- Business Methods ---

    public void flagForComplianceCheck(Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.FLAG_FOR_COMPLIANCE);
        this.setState(PaymentState.PENDING_COMPLIANCE);
        this.registerEvent(new PaymentRequiresComplianceCheck(this.id, this.state, metadata));
    }

    public void recordLimitEarmark(LimitEarmarkResult result, Map<String, Object> metadata) {
        BusinessAction action = (result.status() == LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL)
                ? BusinessAction.RECORD_LIMIT_EARMARK_SUCCESS
                : BusinessAction.RECORD_LIMIT_EARMARK_FAILURE;
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, action);
        this.limitEarmarkResult = result;
        PaymentState paymentState = (action == BusinessAction.RECORD_LIMIT_EARMARK_SUCCESS)
                ? PaymentState.LIMIT_RESERVED
                : PaymentState.LIMIT_COULD_NOT_BE_RESERVED;

        this.setState(paymentState);
        this.registerEvent(new PaymentInProgressEvent(this.id, this.state, metadata));
    }

    public void recordLimitReversal(LimitEarmarkResult result, Map<String, Object> metadata) {
        BusinessAction action = (result.status() == LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_SUCCESSFUL)
                ? BusinessAction.RECORD_LIMIT_REVERSAL_SUCCESS
                : BusinessAction.RECORD_LIMIT_REVERSAL_FAILURE;
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, action);
        this.limitEarmarkResult = result;
        PaymentState paymentState = (action == BusinessAction.RECORD_LIMIT_REVERSAL_SUCCESS)
                ? PaymentState.LIMIT_REVERSED
                : PaymentState.LIMIT_COULD_NOT_BE_REVERSED;

        this.setState(paymentState);
        this.registerEvent(new PaymentInProgressEvent(this.id, this.state, metadata));
    }

    public void recordDebit(DebitLegResult result, Map<String, Object> metadata) {
        BusinessAction action = (result.status() == DebitLegResult.DebitLegStatus.SUCCESSFUL)
                ? BusinessAction.RECORD_DEBIT_SUCCESS
                : BusinessAction.RECORD_DEBIT_FAILURE;
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, action);

        this.debitLegResult = result;
        PaymentState paymentState = (action == BusinessAction.RECORD_DEBIT_SUCCESS)
                ? PaymentState.FUNDS_DEBITED
                : PaymentState.FUNDS_COULD_NOT_BE_DEBITED;

        this.setState(paymentState);
        this.registerEvent(new PaymentInProgressEvent(this.id, this.state, metadata));
    }

    public void recordDebitReversal(DebitLegResult result, Map<String, Object> metadata) {
        BusinessAction action = (result.status() == DebitLegResult.DebitLegStatus.REVERSAL_SUCCESSFUL)
                ? BusinessAction.RECORD_DEBIT_REVERSAL_SUCCESS
                : BusinessAction.RECORD_DEBIT_REVERSAL_FAILURE;
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, action);

        this.debitLegResult = result;
        PaymentState paymentState = (action == BusinessAction.RECORD_DEBIT_REVERSAL_SUCCESS)
                ? PaymentState.FUNDS_DEBIT_REVERSED
                : PaymentState.FUNDS_DEBIT_COULD_NOT_BE_REVERSED;

        this.setState(paymentState);
        this.registerEvent(new PaymentInProgressEvent(this.id, this.state, metadata));
    }

    public void recordCredit(CreditLegResult result, Map<String, Object> metadata) {
        BusinessAction action = (result.status() == CreditLegResult.CreditLegStatus.SUCCESSFUL)
                ? BusinessAction.RECORD_CREDIT_SUCCESS
                : BusinessAction.RECORD_CREDIT_FAILURE;
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, action);
        this.creditLegResult = result;
        PaymentState paymentState = (action == BusinessAction.RECORD_CREDIT_SUCCESS)
                ? PaymentState.FUNDS_CREDITED
                : PaymentState.FUNDS_COULD_NOT_BE_CREDITED;

        this.setState(paymentState);
        this.registerEvent(new PaymentInProgressEvent(this.id, this.state, metadata));
    }

    public void recordPaymentSettled(Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_PAYMENT_SETTLED);
        this.setState(PaymentState.SUCCESS);
        this.registerEvent(new PaymentSuccessfulEvent(this.id, this.state, metadata));
    }

    public void recordPaymentFailed(String reason, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_PAYMENT_FAILED);
        this.setState(PaymentState.FAILED);
        this.registerEvent(new PaymentFailedEvent(this.id, this.state, reason, metadata));
    }

    public void recordPaymentRemediationNeeded(String reason, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_REMEDIATION_NEEDED);

        this.setState(PaymentState.REMEDIATION_NEEDED);
        this.registerEvent(new ManualInterventionRequiredEvent(this.id, this.state, reason, metadata));
    }

    void setState(PaymentState newState) {
        this.state = newState;
        this.updateStatusFromState();
    }

    private void updateStatusFromState() {
        switch (this.state) {
            case NEW -> this.status = Status.NEW;
            case SUCCESS -> this.status = Status.SUCCESSFUL;
            case FAILED -> this.status = Status.FAILED;
            case REMEDIATION_NEEDED -> this.status = Status.REMEDIATION_REQUIRED;
            default -> this.status = Status.IN_PROGRESS;
        }
    }

    public record PaymentCreationParams(UUID transactionId, String transactionReference, String journeyName) {}

    public record PaymentMemento(
            UUID id,
            String transactionReference,
            String journeyName,
            DebitLegResult debitLegResult,
            LimitEarmarkResult limitEarmarkResult,
            CreditLegResult creditLegResult,
            Status status,
            PaymentState state
    ) {}
}
