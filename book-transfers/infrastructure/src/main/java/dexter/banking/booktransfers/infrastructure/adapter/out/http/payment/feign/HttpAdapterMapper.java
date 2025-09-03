package dexter.banking.booktransfers.infrastructure.adapter.out.http.payment.feign;

import dexter.banking.booktransfers.core.domain.payment.valueobject.result.CreditLegResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.DebitLegResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.model.*;
import org.springframework.stereotype.Component;

/**
 * A dedicated Anti-Corruption Layer (ACL) mapper for the outbound HTTP adapter.
 * It translates external DTOs received from Feign clients into pure, internal domain value objects,
 * and translates the core's Parameter Objects into external request DTOs.
 */
@Component
class HttpAdapterMapper {

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

    public LimitManagementRequest toLimitManagementRequest(LimitPort.EarmarkLimitRequest command) {
        return LimitManagementRequest.builder()
                .transactionId(command.transactionId())
                .limitType(command.limitType())
                .build();
    }

    public DepositBankingRequest toDepositBankingRequest(DepositPort.SubmitDepositRequest command) {
        return DepositBankingRequest.builder()
                .transactionId(command.transactionId())
                .accountNumber(command.accountNumber())
                .build();
    }

    public CreditCardBankingRequest toCreditCardBankingRequest(CreditCardPort.SubmitCreditCardPaymentRequest command) {
        return CreditCardBankingRequest.builder()
                .transactionId(command.transactionId())
                .cardNumber(command.cardNumber())
                .build();
    }

    public LimitManagementReversalRequest toLimitEarmarkReversalRequest(LimitPort.ReverseLimitEarmarkRequest command) {
        return LimitManagementReversalRequest.builder()
                .transactionId(command.transactionId())
                .limitManagementId(command.limitManagementId())
                .build();
    }

    public DepositBankingReversalRequest toDepositReversalRequest(DepositPort.SubmitDepositReversalRequest command) {
        return DepositBankingReversalRequest.builder()
                .transactionId(command.transactionId())
                .reservationId(command.reservationId())
                .build();
    }
}
