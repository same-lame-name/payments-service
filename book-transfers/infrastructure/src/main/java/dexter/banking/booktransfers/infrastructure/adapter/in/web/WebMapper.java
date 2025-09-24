package dexter.banking.booktransfers.infrastructure.adapter.in.web;

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

@Mapper(componentModel = "spring", imports = {ModeOfTransfer.class, StringUtils.class, RelId.class, TransactionAmount.class, Currency.class})
interface WebMapper {
    @Mapping(target = "modeOfTransfer", expression = "java(StringUtils.hasText(dto.getModeOfTransfer()) ? ModeOfTransfer.valueOf(dto.getModeOfTransfer().toUpperCase()) : ModeOfTransfer.ASYNC)")
    PaymentCommand toCommand(BookTransferRequest dto, ApiVersion version);

    BookTransferResponse toResponse(PaymentResult paymentResult);
}
