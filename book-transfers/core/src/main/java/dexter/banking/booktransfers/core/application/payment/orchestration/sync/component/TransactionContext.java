package dexter.banking.booktransfers.core.application.payment.orchestration.sync.component;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import dexter.banking.booktransfers.core.application.payment.PaymentCommand;
import dexter.banking.booktransfers.core.application.payment.orchestration.sync.model.ProcessState;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.statemachine.contract.StateMachineContext;
import lombok.Getter;

@Getter
public class TransactionContext implements StateMachineContext<ProcessState> {

    // The live aggregate, used by SYNC state machine actions
    private final transient Payment payment;
    // The initial request, available to all actions
    private final PaymentCommand request;

    private ProcessState currentState;

    @JsonCreator
    public TransactionContext(
            @JsonProperty("payment") Payment payment,
            @JsonProperty("request") PaymentCommand request,
            @JsonProperty("currentState") ProcessState currentState) {
        this.payment = payment;
        this.request = request;
        this.currentState = currentState;
    }

    public TransactionContext(Payment payment, PaymentCommand request) {
        this(payment, request, ProcessState.NEW);
    }


    @Override
    public ProcessState getCurrentState() {
        return this.currentState;
    }

    @Override
    public void setCurrentState(ProcessState newState) {
        this.currentState = newState;
    }

    @Override
    public String getId() {
        return payment.getId().toString();
    }
}
