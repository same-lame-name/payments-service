package dexter.banking.booktransfers.core.domain.model;

import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.domain.primitives.AggregateRoot;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import lombok.Getter;

import java.util.UUID;
import java.util.function.Supplier;

@Getter
public class Payment extends AggregateRoot<UUID> {

    private final String transactionReference;
    // --- State Fields ---
    // The state is now consolidated. Reversal results overwrite the original leg's result.
    private DebitLegResult debitLegResult;
    private LimitEarmarkResult limitEarmarkResult;
    private CreditLegResult creditLegResult;
    private Status status;
    private TransactionState state;

    private Payment(UUID id, String transactionReference, TransactionState state) {
        super(id);
        this.transactionReference = transactionReference;
        this.setState(state);
    }

    public static Payment startNew(PaymentCommand command, Supplier<UUID> idSupplier) {
        return new Payment(idSupplier.get(), command.getTransactionReference(), TransactionState.NEW);
    }

    public static Payment rehydrate(PaymentMemento memento) {
        var payment = new Payment(memento.id(), memento.transactionReference(), memento.state());
        payment.debitLegResult = memento.debitLegResult();
        payment.limitEarmarkResult = memento.limitEarmarkResult();
        payment.creditLegResult = memento.creditLegResult();
        return payment;
    }

    public PaymentMemento getMemento() {
        return new PaymentMemento(
            this.id,
            this.transactionReference,
            this.debitLegResult,
            this.limitEarmarkResult,
            this.creditLegResult,
            this.status,
            this.state
        );
    }

    // --- Type-Safe Data Recording Methods ---

    public void recordLimitEarmarkOutcome(LimitEarmarkResult result) {
        this.limitEarmarkResult = result;
    }

    public void recordDebitLegOutcome(DebitLegResult result) {
        this.debitLegResult = result;
    }

    public void recordCreditResult(CreditLegResult result) {
        this.creditLegResult = result;
    }


    // --- Explicit State Transition Method ---
    public void setState(TransactionState newState) {
        this.state = newState;
        this.updateStatusFromState();
    }

    private void updateStatusFromState() {
        switch (this.state) {
            case NEW:
                this.status = Status.NEW;
                break;
            case TRANSACTION_SUCCESSFUL:
                this.status = Status.SUCCESSFUL;
                break;
            case TRANSACTION_FAILED:
                this.status = Status.FAILED;
                break;
            case MANUAL_INTERVENTION_REQUIRED:
                this.status = Status.REMEDIATION_REQUIRED;
                break;
            default:
                this.status = Status.IN_PROGRESS;
                break;
        }
    }

    public record PaymentMemento(
            UUID id,
            String transactionReference,
            DebitLegResult debitLegResult,
            LimitEarmarkResult limitEarmarkResult,
            CreditLegResult creditLegResult,
            Status status,
            TransactionState state
    ) {}
}
