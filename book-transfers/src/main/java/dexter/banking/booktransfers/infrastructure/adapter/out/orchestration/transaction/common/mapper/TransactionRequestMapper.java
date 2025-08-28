package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper;

import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.LimitManagementRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransactionRequestMapper {

    public LimitManagementRequest toLimitManagementRequest(UUID transactionId, PaymentCommand transactionRequest) {
        return LimitManagementRequest.builder()
                .transactionId(transactionId)
                .limitType(transactionRequest.getLimitType())
                .build();
    }

    public DepositBankingRequest toDepositBankingRequest(UUID transactionId, PaymentCommand transactionRequest) {
        return DepositBankingRequest.builder()
                .transactionId(transactionId)
                .accountNumber(transactionRequest.getAccountNumber())
                .build();
    }

    public CreditCardBankingRequest toCreditCardBankingRequest(UUID transactionId, PaymentCommand transactionRequest) {
        return CreditCardBankingRequest.builder()
                .transactionId(transactionId)
                .cardNumber(transactionRequest.getCardNumber())
                .build();
    }
}
