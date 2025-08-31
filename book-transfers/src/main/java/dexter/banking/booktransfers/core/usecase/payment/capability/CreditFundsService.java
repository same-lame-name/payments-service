package dexter.banking.booktransfers.core.usecase.payment.capability;

import dexter.banking.booktransfers.core.port.TransactionLegPort;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreditFundsService implements CreditFundsUseCase {

    private final TransactionLegPort transactionLegPort;

    @Override
    public void apply(PaymentCommand command) {
        transactionLegPort.sendCreditCardRequest(command);
    }
}
