package dexter.banking.booktransfers.core.domain.model;

import dexter.banking.booktransfers.core.domain.primitives.AggregateRoot;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.LimitManagementResponse;
import lombok.Getter;

import java.util.UUID;
import java.util.function.Supplier;

@Getter
public class Payment extends AggregateRoot<UUID> {

    private final String transactionReference;

    // --- State Fields ---
    private DepositBankingResponse depositBankingResponse;
    private LimitManagementResponse limitManagementResponse;
    private CreditCardBankingResponse creditCardBankingResponse;
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
        payment.depositBankingResponse = memento.depositBankingResponse();
        payment.limitManagementResponse = memento.limitManagementResponse();
        payment.creditCardBankingResponse = memento.creditCardBankingResponse();
        return payment;
    }

    public PaymentMemento getMemento() {
        return new PaymentMemento(
            this.id,
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
            DepositBankingResponse depositBankingResponse,
            LimitManagementResponse limitManagementResponse,
            CreditCardBankingResponse creditCardBankingResponse,
            Status status,
            TransactionState state
    ) {}
}
