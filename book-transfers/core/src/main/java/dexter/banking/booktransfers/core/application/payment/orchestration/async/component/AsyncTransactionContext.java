package dexter.banking.booktransfers.core.application.payment.orchestration.async.component;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dexter.banking.booktransfers.core.application.payment.orchestration.async.model.AsyncProcessState;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.ModeOfTransfer;
import dexter.banking.statemachine.contract.StateMachineContext;
import lombok.Getter;

import java.util.UUID;

/**
 * A lean, serializable context object specifically for the asynchronous state machine.
 * It contains only flattened, persistable data, not live domain objects, making it stable for persistence.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class AsyncTransactionContext implements StateMachineContext<AsyncProcessState> {

    private final UUID paymentId;
    private AsyncProcessState currentState;

    // --- Flattened PaymentCommand fields ---
    private final UUID idempotencyKey;
    private final String transactionReference;
    private final String limitType;
    private final String accountNumber;
    private final String cardNumber;
    private final String webhookUrl;
    private final String realtime;
    private final ModeOfTransfer modeOfTransfer;
    private final ApiVersion version;


    @JsonCreator
    public AsyncTransactionContext(
            @JsonProperty("paymentId") UUID paymentId,
            @JsonProperty("currentState") AsyncProcessState currentState,
            @JsonProperty("idempotencyKey") UUID idempotencyKey,
            @JsonProperty("transactionReference") String transactionReference,
            @JsonProperty("limitType") String limitType,
            @JsonProperty("accountNumber") String accountNumber,
            @JsonProperty("cardNumber") String cardNumber,
            @JsonProperty("webhookUrl") String webhookUrl,
            @JsonProperty("realtime") String realtime,
            @JsonProperty("modeOfTransfer") ModeOfTransfer modeOfTransfer,
            @JsonProperty("version") ApiVersion version
    ) {
        this.paymentId = paymentId;
        this.currentState = currentState;
        this.idempotencyKey = idempotencyKey;
        this.transactionReference = transactionReference;
        this.limitType = limitType;
        this.accountNumber = accountNumber;
        this.cardNumber = cardNumber;
        this.webhookUrl = webhookUrl;
        this.realtime = realtime;
        this.modeOfTransfer = modeOfTransfer;
        this.version = version;
    }

    @Override
    public AsyncProcessState getCurrentState() {
        return this.currentState;
    }

    @Override
    public void setCurrentState(AsyncProcessState newState) {
        this.currentState = newState;
    }

    @Override
    public String getId() {
        return paymentId.toString();
    }
}
