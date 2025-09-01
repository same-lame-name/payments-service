package dexter.banking.booktransfers.infrastructure.adapter.out.messaging;

import dexter.banking.booktransfers.core.application.payment.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.model.CreditCardBankingRequest;
import dexter.banking.model.DepositBankingRequest;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import org.springframework.stereotype.Component;

/**
 * A dedicated Anti-Corruption Layer (ACL) mapper for the outbound JMS adapter.
 * It translates the core's PaymentCommand into external request DTOs for messaging.
 */
@Component
public class JmsAdapterMapper {
    public LimitManagementRequest toLimitManagementRequest(PaymentCommand command) {
        return LimitManagementRequest.builder()
                .transactionId(command.getTransactionId())
                .limitType(command.getLimitType())
                .build();
    }

    public DepositBankingRequest toDepositBankingRequest(PaymentCommand command) {
        return DepositBankingRequest.builder()
                .transactionId(command.getTransactionId())
                .accountNumber(command.getAccountNumber())
                .build();
    }

    public CreditCardBankingRequest toCreditCardBankingRequest(PaymentCommand command) {
        return CreditCardBankingRequest.builder()
                .transactionId(command.getTransactionId())
                .cardNumber(command.getCardNumber())
                .build();
    }

    public LimitManagementReversalRequest toLimitEarmarkReversalRequest(Payment payment) {
        return LimitManagementReversalRequest.builder()
                .transactionId(payment.getId())
                .limitManagementId(payment.getLimitEarmarkResult().limitId())
                .build();
    }

    public DepositBankingReversalRequest toDepositReversalRequest(Payment payment) {
        return DepositBankingReversalRequest.builder()
                .transactionId(payment.getId())
                .reservationId(payment.getDebitLegResult().depositId())
                .build();
    }
}
