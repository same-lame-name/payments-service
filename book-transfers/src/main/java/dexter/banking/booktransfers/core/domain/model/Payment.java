package dexter.banking.booktransfers.core.domain.model;

import dexter.banking.booktransfers.core.domain.event.ManualInterventionRequiredEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentFailedEvent;
import dexter.banking.booktransfers.core.domain.event.PaymentSuccessfulEvent;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessAction;
import dexter.banking.booktransfers.core.domain.model.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.model.policy.PolicyEvaluationContext;
import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.domain.primitives.AggregateRoot;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
public class Payment extends AggregateRoot<UUID> {

    private final String transactionReference;
    private final String journeyName; // The identifier for the business process context.
    private final transient BusinessPolicy policy; // The injected guardian of this aggregate's state.

    // --- State Fields ---
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

    public static Payment startNew(PaymentCommand command, BusinessPolicy policy, String journeyName) {
        var payment = new Payment(command.getIdempotencyKey(), command.getTransactionReference(), journeyName, policy, PaymentState.NEW);
        PolicyEvaluationContext context = new PolicyEvaluationContext(payment.getMemento(), null);
        policy.evaluate(context, BusinessAction.START_PAYMENT); // Enforce starting rules
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

    public void recordLimitEarmarkSuccess(LimitEarmarkResult result, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_LIMIT_EARMARK_SUCCESS);
        this.limitEarmarkResult = result;
        this.setState(PaymentState.LIMIT_RESERVED);
    }

    public void recordLimitEarmarkFailure(LimitEarmarkResult result, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_LIMIT_EARMARK_FAILURE);
        this.limitEarmarkResult = result;
        this.setState(PaymentState.FAILED);
        this.registerEvent(new PaymentFailedEvent(this.id, "Limit earmark failed", metadata));
    }

    public void recordDebitSuccess(DebitLegResult result, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_DEBIT_SUCCESS);
        this.debitLegResult = result;
        this.setState(PaymentState.FUNDS_DEBITED);
    }

    public void recordDebitFailure(DebitLegResult result, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_DEBIT_FAILURE);
        this.debitLegResult = result;

        this.setState(PaymentState.FAILED);
        this.registerEvent(new PaymentFailedEvent(this.id, "Debit leg failed", metadata));
    }

    public void recordCreditSuccess(CreditLegResult result, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_CREDIT_SUCCESS);
        this.creditLegResult = result;
        this.setState(PaymentState.SETTLED);
        this.registerEvent(new PaymentSuccessfulEvent(this.id, metadata));
    }

    public void recordCreditFailure(CreditLegResult result, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_CREDIT_FAILURE);
        this.creditLegResult = result;

        this.setState(PaymentState.FAILED);
        this.registerEvent(new PaymentFailedEvent(this.id, "Debit leg failed", metadata));
    }

    public void recordDebitReversalSuccess(DebitLegResult result, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_DEBIT_REVERSAL_SUCCESS);
        this.debitLegResult = result;
    }

    public void recordDebitReversalFailure(DebitLegResult result, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_DEBIT_REVERSAL_FAILURE);
        this.debitLegResult = result;
        this.setState(PaymentState.REMEDIATION_NEEDED);
        this.registerEvent(new ManualInterventionRequiredEvent(this.id, "Debit reversal failed", metadata));
    }

    public void recordLimitReversalSuccess(LimitEarmarkResult result, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_LIMIT_REVERSAL_SUCCESS);
        this.limitEarmarkResult = result;
    }

    public void recordLimitReversalFailure(LimitEarmarkResult result, Map<String, Object> metadata) {
        var context = new PolicyEvaluationContext(this.getMemento(), metadata);
        this.policy.evaluate(context, BusinessAction.RECORD_LIMIT_REVERSAL_FAILURE);
        this.limitEarmarkResult = result;
        this.setState(PaymentState.REMEDIATION_NEEDED);
        this.registerEvent(new ManualInterventionRequiredEvent(this.id, "Limit reversal failed", metadata));
    }


    void setState(PaymentState newState) {
        this.state = newState;
        this.updateStatusFromState();
    }

    private void updateStatusFromState() {
        switch (this.state) {
            case NEW, PENDING_COMPLIANCE -> this.status = Status.NEW;
            case SETTLED -> this.status = Status.SUCCESSFUL;
            case FAILED -> this.status = Status.FAILED;
            case REMEDIATION_NEEDED -> this.status = Status.REMEDIATION_REQUIRED;
            default -> this.status = Status.IN_PROGRESS;
        }
    }

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
