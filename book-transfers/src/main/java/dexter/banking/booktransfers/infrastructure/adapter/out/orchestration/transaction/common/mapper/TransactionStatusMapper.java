package dexter.banking.booktransfers.infrastructure.adapter.out.orchestration.transaction.common.mapper;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TransactionStatusMapper {

    public LimitManagementReversalRequest toLimitEarmarkReversalRequest(UUID transactionId, Payment payment) {
        return LimitManagementReversalRequest.builder()
                .transactionId(transactionId)
                .limitManagementId(payment.getLimitManagementResponse().getLimitId())
                .build();
    }

    public DepositBankingReversalRequest toDepositReversalRequest(UUID transactionId, Payment payment) {
        return DepositBankingReversalRequest.builder()
                .transactionId(transactionId)
                .reservationId(payment.getDepositBankingResponse().getDepositId())
                .build();
    }
}
