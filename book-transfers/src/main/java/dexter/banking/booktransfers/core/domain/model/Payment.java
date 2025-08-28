package dexter.banking.booktransfers.core.domain.model;

import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.LimitManagementResponse;
import lombok.Getter;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * The Aggregate Root for a payment transaction.
 *
 * This is a pure domain object. Its responsibility is to represent the current state of a transaction
 * and protect its own consistency.
 *
 * It has no knowledge of workflows, processes, or infrastructure. The Orchestrator (e.g., a Command Handler)
 * is responsible for telling it what happened and what state to transition to.
 */
@Getter
public class Payment {

    private final UUID transactionId;
    private final String transactionReference;

    // --- State Fields ---
    private DepositBankingResponse depositBankingResponse;
    private LimitManagementResponse limitManagementResponse;
    private CreditCardBankingResponse creditCardBankingResponse;
    private Status status;
    private TransactionState state;

    /**
     * Private constructor for internal use by the factories.
     */
    private Payment(UUID transactionId, String transactionReference, TransactionState state) {
        this.transactionId = transactionId;
        this.transactionReference = transactionReference;
        this.setState(state);
    }

    /**
     * Factory method for creating a brand new Payment aggregate.
     */
    public static Payment startNew(PaymentCommand command, Supplier<UUID> idSupplier) {
        return new Payment(idSupplier.get(), command.getTransactionReference(), TransactionState.NEW);
    }

    /**
     * Factory method for rehydrating an existing Payment aggregate from persistence.
     * This is the type-safe, reflection-free entry point for the persistence layer.
     */
    public static Payment rehydrate(PaymentMemento memento) {
        var payment = new Payment(memento.transactionId(), memento.transactionReference(), memento.state());
        payment.depositBankingResponse = memento.depositBankingResponse();
        payment.limitManagementResponse = memento.limitManagementResponse();
        payment.creditCardBankingResponse = memento.creditCardBankingResponse();
        return payment;
    }

    /**
     * Creates a Memento snapshot of the aggregate's current state for persistence.
     */
    public PaymentMemento getMemento() {
        return new PaymentMemento(
            this.transactionId,
            this.transactionReference,
            this.depositBankingResponse,
            this.limitManagementResponse,
            this.creditCardBankingResponse,
            this.status,
            this.state
        );
    }

    // --- Type-Safe Data Recording Methods ---

    public void recordLimitEarmarkResult(LimitManagementResponse response) {
        this.limitManagementResponse = response;
    }

    public void recordDebitResult(DepositBankingResponse response) {
        this.depositBankingResponse = response;
    }

    public void recordCreditResult(CreditCardBankingResponse response) {
        this.creditCardBankingResponse = response;
    }

    public void recordDebitReversalResult(DepositBankingResponse response) {
        this.depositBankingResponse = response;
    }

    public void recordLimitReversalResult(LimitManagementResponse response) {
        this.limitManagementResponse = response;
    }


    // --- Explicit State Transition Method ---

    /**
     * The single, explicit method for changing the aggregate's state.
     * This method contains no validation logic; it trusts the calling orchestrator
     * to provide a valid next state based on the workflow.
     * @param newState The state to transition to, as determined by the orchestrator.
     */
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

    /**
     * A Memento representing a snapshot of the Payment aggregate's internal state for persistence.
     */
    public record PaymentMemento(
            UUID transactionId,
            String transactionReference,
            DepositBankingResponse depositBankingResponse,
            LimitManagementResponse limitManagementResponse,
            CreditCardBankingResponse creditCardBankingResponse,
            Status status,
            TransactionState state
    ) {}
}
