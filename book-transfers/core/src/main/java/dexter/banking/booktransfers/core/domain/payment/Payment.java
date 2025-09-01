package dexter.banking.booktransfers.core.domain.payment;


import dexter.banking.booktransfers.core.application.payment.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.event.ManualInterventionRequiredEvent;
import dexter.banking.booktransfers.core.domain.payment.event.PaymentFailedEvent;
import dexter.banking.booktransfers.core.domain.payment.event.PaymentInProgressEvent;
import dexter.banking.booktransfers.core.domain.payment.event.PaymentSuccessfulEvent;
import dexter.banking.booktransfers.core.domain.payment.result.CreditLegResult;
import dexter.banking.booktransfers.core.domain.payment.result.DebitLegResult;
import dexter.banking.booktransfers.core.domain.payment.result.LimitEarmarkResult;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessAction;
import dexter.banking.booktransfers.core.domain.shared.policy.BusinessPolicy;
import dexter.banking.booktransfers.core.domain.shared.policy.PolicyEvaluationContext;
import dexter.banking.booktransfers.core.domain.shared.primitives.AggregateRoot;
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
        var payment = new Payment(command.getTransactionId(), command.getTransactionReference(), journeyName, policy, PaymentState.NEW);
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
        this.setState(PaymentState.SETTLED);
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
