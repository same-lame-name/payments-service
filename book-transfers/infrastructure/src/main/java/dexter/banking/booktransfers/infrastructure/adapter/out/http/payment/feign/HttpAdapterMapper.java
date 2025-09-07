package dexter.banking.booktransfers.infrastructure.adapter.out.http.payment.feign;

import dexter.banking.booktransfers.core.domain.payment.valueobject.result.CreditLegResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.DebitLegResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.result.LimitEarmarkResult;
import dexter.banking.booktransfers.core.port.out.CreditCardPort;
import dexter.banking.booktransfers.core.port.out.DepositPort;
import dexter.banking.booktransfers.core.port.out.LimitPort;
import dexter.banking.model.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * A dedicated Anti-Corruption Layer (ACL) mapper for the outbound HTTP adapter.
 * It translates external DTOs received from Feign clients into pure, internal domain value objects,
 * and translates the core's Parameter Objects into external request DTOs.
 */
@Mapper(componentModel = "spring", imports = {LimitEarmarkStatus.class, DepositBankingStatus.class, CreditCardBankingStatus.class})
interface HttpAdapterMapper {

    @Mapping(target = "status", expression = "java(dto.getStatus() == CreditCardBankingStatus.SUCCESSFUL ? CreditLegResult.CreditLegStatus.SUCCESSFUL : CreditLegResult.CreditLegStatus.FAILED)")
    @Mapping(target = "creditCardRequestId", source = "creditCardBankingId")
    CreditLegResult toDomain(CreditCardBankingResponse dto);

    @Mapping(target = "status", expression = "java(dto.getStatus() == DepositBankingStatus.SUCCESSFUL ? DebitLegResult.DebitLegStatus.SUCCESSFUL : DebitLegResult.DebitLegStatus.FAILED)")
    @Mapping(target = "depositId", source = "depositId")
    DebitLegResult toDomain(DepositBankingResponse dto);

    @Mapping(target = "status", expression = "java(dto.getStatus() == DepositBankingStatus.REVERSAL_SUCCESSFUL ? DebitLegResult.DebitLegStatus.REVERSAL_SUCCESSFUL : DebitLegResult.DebitLegStatus.REVERSAL_FAILED)")
    @Mapping(target = "depositId", source = "depositId")
    DebitLegResult toReversalDomain(DepositBankingResponse dto);

    @Mapping(target = "status", expression = "java(dto.getStatus() == LimitEarmarkStatus.SUCCESSFUL ? LimitEarmarkResult.LimitEarmarkStatus.SUCCESSFUL : LimitEarmarkResult.LimitEarmarkStatus.FAILED)")
    @Mapping(target = "limitId", source = "limitId")
    LimitEarmarkResult toDomain(LimitManagementResponse dto);

    @Mapping(target = "status", expression = "java(dto.getStatus() == LimitEarmarkStatus.REVERSAL_SUCCESSFUL ? LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_SUCCESSFUL : LimitEarmarkResult.LimitEarmarkStatus.REVERSAL_FAILED)")
    @Mapping(target = "limitId", source = "limitId")
    LimitEarmarkResult toReversalDomain(LimitManagementResponse dto);

    LimitManagementRequest toLimitManagementRequest(LimitPort.EarmarkLimitRequest command);

    DepositBankingRequest toDepositBankingRequest(DepositPort.SubmitDepositRequest command);

    CreditCardBankingRequest toCreditCardBankingRequest(CreditCardPort.SubmitCreditCardPaymentRequest command);

    LimitManagementReversalRequest toLimitEarmarkReversalRequest(LimitPort.ReverseLimitEarmarkRequest command);

    DepositBankingReversalRequest toDepositReversalRequest(DepositPort.SubmitDepositReversalRequest command);
}
