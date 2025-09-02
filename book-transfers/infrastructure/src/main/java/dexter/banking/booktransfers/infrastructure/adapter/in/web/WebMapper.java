package dexter.banking.booktransfers.infrastructure.adapter.in.web;

import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
class WebMapper {
    public PaymentCommand toCommand(BookTransferRequest dto, ApiVersion version) {
        if (dto == null) {
            return null;
        }

        UUID transactionId = UUID.randomUUID(); // Generate a new transaction ID here if needed
        return PaymentCommand.builder()
                .transactionId(transactionId)
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
