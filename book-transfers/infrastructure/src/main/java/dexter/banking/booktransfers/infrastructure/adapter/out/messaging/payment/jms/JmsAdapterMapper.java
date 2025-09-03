package dexter.banking.booktransfers.infrastructure.adapter.out.messaging.payment.jms;

import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import org.springframework.stereotype.Component;
/**
 * A dedicated Anti-Corruption Layer (ACL) mapper for the outbound JMS adapter.
 * It translates the core's lean Parameter Objects into external request DTOs for messaging.
 */
@Component
class JmsAdapterMapper {
    public LimitManagementRequest toLimitManagementRequest(LimitPort.EarmarkLimitRequest request) {
        return LimitManagementRequest.builder()
                .transactionId(request.transactionId())
                .limitType(request.limitType())
                .build();
    }

    public DepositBankingRequest toDepositBankingRequest(DepositPort.SubmitDepositRequest request) {
        return DepositBankingRequest.builder()
                .transactionId(request.transactionId())
                .accountNumber(request.accountNumber())
                .build();
    }

    public CreditCardBankingRequest toCreditCardBankingRequest(CreditCardPort.SubmitCreditCardPaymentRequest request) {
        return CreditCardBankingRequest.builder()
                .transactionId(request.transactionId())
                .cardNumber(request.cardNumber())
                .build();
    }

    public LimitManagementReversalRequest toLimitEarmarkReversalRequest(LimitPort.ReverseLimitEarmarkRequest request) {
        return LimitManagementReversalRequest.builder()
                .transactionId(request.transactionId())
                .limitManagementId(request.limitManagementId())
                .build();
    }

    public DepositBankingReversalRequest toDepositReversalRequest(DepositPort.SubmitDepositReversalRequest request) {
        return DepositBankingReversalRequest.builder()
                .transactionId(request.transactionId())
                .reservationId(request.reservationId())
                .build();
    }
}
