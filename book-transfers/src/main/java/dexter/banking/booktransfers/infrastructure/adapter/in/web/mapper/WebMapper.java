package dexter.banking.booktransfers.infrastructure.adapter.in.web.mapper;

import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.model.PaymentResult;
import dexter.banking.booktransfers.core.usecase.payment.PaymentCommand;
import dexter.banking.booktransfers.infrastructure.adapter.in.web.dto.BookTransferRequest;
import dexter.banking.booktransfers.infrastructure.adapter.in.web.dto.BookTransferResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WebMapper {
    public PaymentCommand toCommand(BookTransferRequest dto, ApiVersion version) {
        if (dto == null) {
            return null;
        }
        return PaymentCommand.builder()
                .idempotencyKey(dto.getIdempotencyKey())
                .transactionReference(dto.getTransactionReference())
                .limitType(dto.getLimitType())
                .accountNumber(dto.getAccountNumber())
                .cardNumber(dto.getCardNumber())
                .webhookUrl(dto.getWebhookUrl())
                .realtime(dto.getRealtime())
                .modeOfTransfer(StringUtils.hasText(dto.getModeOfTransfer()) ? ModeOfTransfer.valueOf(dto.getModeOfTransfer().toUpperCase()) : ModeOfTransfer.ASYNC)
                .version(version) // Set the version from the controller
                .build();
    }

    public BookTransferResponse toResponse(PaymentResult paymentResult) {
        if (paymentResult == null) {
            return null;
        }

        return BookTransferResponse.builder()
                .transactionId(paymentResult.transactionId())
                .status(paymentResult.status())
                .state(paymentResult.state())
                .build();
    }
}
