package dexter.banking.booktransfers.infrastructure.adapter.in.web;
import dexter.banking.booktransfers.core.application.compliance.command.RejectComplianceCaseCommand;
import dexter.banking.booktransfers.core.application.payment.command.HighValuePaymentCommand;
import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.RelId;
import dexter.banking.booktransfers.core.domain.payment.valueobject.TransactionAmount;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.util.StringUtils;

import java.util.Currency;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {ModeOfTransfer.class, StringUtils.class, RelId.class, TransactionAmount.class, Currency.class})
interface WebMapper {
    @Mapping(target = "modeOfTransfer", expression = "java(StringUtils.hasText(dto.getModeOfTransfer()) ? ModeOfTransfer.valueOf(dto.getModeOfTransfer().toUpperCase()) : ModeOfTransfer.ASYNC)")
    PaymentCommand toCommand(BookTransferRequest dto, ApiVersion version);

    @Mapping(target = "relId", expression = "java(new RelId(dto.getRelId()))")
    @Mapping(target = "transactionAmount", expression = "java(new TransactionAmount(dto.getAmount(), Currency.getInstance(dto.getCurrency())))")
    HighValuePaymentCommand toCommand(BookTransferRequestV3 dto);

    RejectComplianceCaseCommand toCommand(UUID complianceCaseId, RejectComplianceRequest dto);

    BookTransferResponse toResponse(PaymentResult paymentResult);
}
