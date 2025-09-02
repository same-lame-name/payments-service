package dexter.banking.booktransfers.core.application.payment.service;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.port.in.payment.CreditFundsUseCase;
import dexter.banking.booktransfers.core.port.out.TransactionLegPort;
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
