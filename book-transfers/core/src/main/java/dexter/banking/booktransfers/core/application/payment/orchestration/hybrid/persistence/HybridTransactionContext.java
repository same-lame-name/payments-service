package dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.persistence;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dexter.banking.booktransfers.core.application.payment.orchestration.hybrid.model.ProcessStateV3;
import dexter.banking.booktransfers.core.domain.payment.valueobject.RelId;
import dexter.banking.booktransfers.core.domain.payment.valueobject.TransactionAmount;
import dexter.banking.statemachine.contract.StateMachineContext;
import lombok.Getter;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class HybridTransactionContext implements StateMachineContext<ProcessStateV3> {

    private final UUID paymentId;
    private ProcessStateV3 currentState;

    // --- Flattened HighValuePaymentCommand fields ---
    private final UUID idempotencyKey;
    private final String transactionReference;
    private final RelId relId;
    private final TransactionAmount transactionAmount;
    private final String limitType;
    private final String accountNumber;
    private final String cardNumber;

    @JsonCreator
    public HybridTransactionContext(
            @JsonProperty("paymentId") UUID paymentId,
            @JsonProperty("currentState") ProcessStateV3 currentState,
            @JsonProperty("idempotencyKey") UUID idempotencyKey,
            @JsonProperty("transactionReference") String transactionReference,
            @JsonProperty("relId") RelId relId,
            @JsonProperty("transactionAmount") TransactionAmount transactionAmount,
            @JsonProperty("limitType") String limitType,
            @JsonProperty("accountNumber") String accountNumber,
            @JsonProperty("cardNumber") String cardNumber
    ) {
        this.paymentId = paymentId;
        this.currentState = currentState;
        this.idempotencyKey = idempotencyKey;
        this.transactionReference = transactionReference;
        this.relId = relId;
        this.transactionAmount = transactionAmount;
        this.limitType = limitType;
        this.accountNumber = accountNumber;
        this.cardNumber = cardNumber;
    }

    @Override
    public ProcessStateV3 getCurrentState() {
        return this.currentState;
    }

    @Override
    public void setCurrentState(ProcessStateV3 newState) {
        this.currentState = newState;
    }

    @Override
    public String getId() {
        return paymentId.toString();
    }
}
