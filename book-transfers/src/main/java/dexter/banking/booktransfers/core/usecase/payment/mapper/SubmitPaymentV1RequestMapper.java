package dexter.banking.booktransfers.core.usecase.payment.mapper;

import dexter.banking.booktransfers.core.domain.model.PaymentCommand;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.LimitManagementRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A dedicated mapper for the V1 procedural use case.
 * Maps the command to specific request objects for outbound ports.
 */
@Component
public class SubmitPaymentV1RequestMapper {

    public LimitManagementRequest toLimitManagementRequest(UUID transactionId, PaymentCommand command) {
        return LimitManagementRequest.builder()
                .transactionId(transactionId)
                .limitType(command.getLimitType())
                .build();
    }

    public DepositBankingRequest toDepositBankingRequest(UUID transactionId, PaymentCommand command) {
        return DepositBankingRequest.builder()
                .transactionId(transactionId)
                .accountNumber(command.getAccountNumber())
                .build();
    }

    public CreditCardBankingRequest toCreditCardBankingRequest(UUID transactionId, PaymentCommand command) {
        return CreditCardBankingRequest.builder()
                .transactionId(transactionId)
                .cardNumber(command.getCardNumber())
                .build();
    }
}
