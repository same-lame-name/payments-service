package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.statemachine.contract.StateMachineContext;
import lombok.Getter;

@Getter
public class TransactionContext implements StateMachineContext<TransactionState> {

    private final Payment payment;
    private final PaymentCommand request;

    @JsonCreator
    public TransactionContext(
            @JsonProperty("payment") Payment payment,
            @JsonProperty("request") PaymentCommand request) {
        this.payment = payment;
        this.request = request;
    }

    @Override
    public TransactionState getCurrentState() {
        return payment.getState();
    }

    @Override
    public void setCurrentState(TransactionState newState) {
        payment.setState(newState);
    }

    @Override
    public String getId() {
        return payment.getId().toString();
    }
}
