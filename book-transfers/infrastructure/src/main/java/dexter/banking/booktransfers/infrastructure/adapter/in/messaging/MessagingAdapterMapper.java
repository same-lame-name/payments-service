package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.domain.payment.result.CreditLegResult;
import dexter.banking.booktransfers.core.domain.payment.result.DebitLegResult;
import dexter.banking.booktransfers.core.domain.payment.result.LimitEarmarkResult;
import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.CreditCardBankingStatus;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.DepositBankingStatus;
import dexter.banking.model.LimitEarmarkStatus;
import dexter.banking.model.LimitManagementResponse;
import org.springframework.stereotype.Component;

/**
 * A dedicated Anti-Corruption Layer (ACL) mapper for the inbound messaging adapter.
 * It translates external DTOs received from JMS queues into pure, internal domain value objects.
 */
@Component
public class MessagingAdapterMapper {

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
}
