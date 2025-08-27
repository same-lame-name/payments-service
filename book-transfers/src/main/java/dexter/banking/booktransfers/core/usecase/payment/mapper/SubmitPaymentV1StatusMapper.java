package dexter.banking.booktransfers.core.usecase.payment.mapper;

import dexter.banking.booktransfers.core.domain.model.PaymentResponse;
import dexter.banking.model.DepositBankingReversalRequest;
import dexter.banking.model.LimitManagementReversalRequest;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A dedicated mapper for the V1 procedural use case.
 * Maps the current state of a transaction to reversal request objects for Saga compensation.
 */
@Component
public class SubmitPaymentV1StatusMapper {

    public LimitManagementReversalRequest toLimitEarmarkReversalRequest(UUID transactionId, PaymentResponse paymentResponse) {
        return LimitManagementReversalRequest.builder()
                .transactionId(transactionId)
                .limitManagementId(paymentResponse.getLimitManagementResponse().getLimitId())
                .build();
    }

    public DepositBankingReversalRequest toDepositReversalRequest(UUID transactionId, PaymentResponse paymentResponse) {
        return DepositBankingReversalRequest.builder()
                .transactionId(transactionId)
                .reservationId(paymentResponse.getDepositBankingResponse().getDepositId())
                .build();
    }
}
