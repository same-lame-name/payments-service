package dexter.banking.booktransfers.infrastructure.adapter.in.web;
import dexter.banking.booktransfers.core.application.compliance.command.RejectComplianceCaseCommand;
import dexter.banking.booktransfers.core.application.payment.command.HighValuePaymentCommand;
import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.ModeOfTransfer;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.payment.valueobject.RelId;
import dexter.banking.booktransfers.core.domain.payment.valueobject.TransactionAmount;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Currency;
import java.util.UUID;

@Component
class WebMapper {
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
                .version(version)
                .build();
    }

    public HighValuePaymentCommand toCommand(BookTransferRequestV3 dto) {
        if (dto == null) {
            return null;
        }

        return HighValuePaymentCommand.builder()
                .idempotencyKey(dto.getIdempotencyKey())
                .transactionReference(dto.getTransactionReference())
                .relId(new RelId(dto.getRelId()))
                .transactionAmount(new TransactionAmount(dto.getAmount(), Currency.getInstance(dto.getCurrency())))
                .limitType(dto.getLimitType())
                .accountNumber(dto.getAccountNumber())
                .cardNumber(dto.getCardNumber())
                .build();
    }

    public RejectComplianceCaseCommand toCommand(UUID caseId, RejectComplianceRequest dto) {
        if (dto == null) {
            return null;
        }
        return new RejectComplianceCaseCommand(caseId, dto.getReason());
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
