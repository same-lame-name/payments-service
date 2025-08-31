package dexter.banking.booktransfers.infrastructure.adapter.out.http.mapper;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.model.*;
import org.springframework.stereotype.Component;

/**
 * A dedicated Anti-Corruption Layer (ACL) mapper for the outbound HTTP adapter.
 * It translates external DTOs received from Feign clients into pure, internal domain value objects,
 * and translates the core's PaymentCommand into external request DTOs.
 */
@Component
public class HttpAdapterMapper {

    public CreditLegResult toDomain(CreditCardBankingResponse dto) {
        CreditLegResult.CreditLegStatus status = (dto.getStatus() == CreditCardBankingStatus.SUCCESSFUL)
                ? CreditLegResult.CreditLegStatus.SUCCESSFUL
                : CreditLegResult.CreditLegStatus.FAILED;
        return new CreditLegResult(dto.getCreditCardBankingId(), status);
    }

    public DebitLegResult toDomain(DepositBankingResponse dto) {
        DebitLegResult.DebitLegStatus status = (dto.getStatus() == DepositBankingStatus.SUCCESSFUL)
                ? DebitLegResult.DebitLegStatus.SUCCESSFUL
                : DebitLegResult.DebitLegStatus.FAILED;
        return new DebitLegResult(dto.getDepositId(), status);
    }

    public DebitLegResult toReversalDomain(DepositBankingResponse dto) {
        DebitLegResult.DebitLegStatus status = (dto.getStatus() == DepositBankingStatus.REVERSAL_SUCCESSFUL)
                ? DebitLegResult.DebitLegStatus.REVERSAL_SUCCESSFUL
                : DebitLegResult.DebitLegStatus.REVERSAL_FAILED;
        return new DebitLegResult(dto.getDepositId(), status);
    }

    public LimitEarmarkResult toDomain(LimitManagementResponse dto) {
        LimitEarmarkResult.LimitEarmarkStatus status = (dto.getStatus() == LimitEarmarkStatus.SUCCESSFUL)
                ? LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL
                : LimitEarmarkResult.LimitEarmarkStatus.FAILED;
        return new LimitEarmarkResult(dto.getLimitId(), status);
    }

    public LimitEarmarkResult toReversalDomain(LimitManagementResponse dto) {
        LimitEarmarkResult.LimitEarmarkStatus status = (dto.getStatus() == LimitEarmarkStatus.REVERSAL_SUCCESSFUL)
                ? LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_SUCCESSFUL
                : LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_FAILED;
        return new LimitEarmarkResult(dto.getLimitId(), status);
    }

    public LimitManagementRequest toLimitManagementRequest(PaymentCommand command) {
        return LimitManagementRequest.builder()
                .transactionId(command.getIdempotencyKey())
                .limitType(command.getLimitType())
                .build();
    }

    public DepositBankingRequest toDepositBankingRequest(PaymentCommand command) {
        return DepositBankingRequest.builder()
                .transactionId(command.getIdempotencyKey())
                .accountNumber(command.getAccountNumber())
                .build();
    }

    public CreditCardBankingRequest toCreditCardBankingRequest(PaymentCommand command) {
        return CreditCardBankingRequest.builder()
                .transactionId(command.getIdempotencyKey())
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
