package dexter.banking.booktransfers.core.domain.model;

import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.LimitManagementResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private UUID transactionId;
    private String transactionReference; // Added to be persisted for query purposes

    private DepositBankingResponse depositBankingResponse;
    private LimitManagementResponse limitManagementResponse;
    private CreditCardBankingResponse creditCardBankingResponse;

    @Builder.Default
    private Status status = Status.NEW;
    @Builder.Default
    private TransactionState state = TransactionState.NEW;

    /**
     * This method is now the single, authoritative way to change the state.
     * By placing the status-mapping logic here, we ensure the domain object
     * is always consistent. This is a core principle of DDD.
     *
     * @param newState The new technical state for the transaction.
     */
    public void setState(TransactionState newState) {
        this.state = newState;
        this.updateStatusFromState(); // Automatically update the business status
    }

    /**
     * Encapsulated business logic to derive the business-facing Status from
     * the FSM's internal TransactionState. This logic was moved from the deleted
     * StatusSyncListener.
     */
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
}
