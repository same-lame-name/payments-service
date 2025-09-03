package dexter.banking.booktransfers.infrastructure.adapter.in.web;

import dexter.banking.booktransfers.core.application.compliance.command.ApproveComplianceCaseCommand;
import dexter.banking.booktransfers.core.application.compliance.command.RejectComplianceCaseCommand;
import dexter.banking.booktransfers.core.application.compliance.query.ComplianceCaseView;
import dexter.banking.booktransfers.core.application.payment.command.HighValuePaymentCommand;
import dexter.banking.booktransfers.core.application.payment.command.PaymentCommand;
import dexter.banking.booktransfers.core.application.payment.query.PaymentView;
import dexter.banking.booktransfers.core.domain.payment.ApiVersion;
import dexter.banking.booktransfers.core.domain.payment.PaymentResult;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.port.in.compliance.ComplianceQueryUseCase;
import dexter.banking.booktransfers.core.port.in.payment.PaymentQueryUseCase;
import dexter.banking.commandbus.CommandBus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * The primary Driving Adapter for the REST API.
 * It now uses a dedicated query stack (PaymentQueryUseCase -> PaymentView) for reads,
 * and the command stack (CommandBus -> BookTransferResponse) for writes.
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
class BookTransferController {

    private final CommandBus commandBus;
    private final PaymentQueryUseCase paymentQueryUseCase;
    private final ComplianceQueryUseCase complianceQueryUseCase;
    private final WebMapper webMapper;

    @PostMapping("/v1/book-transfers/payment")
    public BookTransferResponse submitTransactionV1(@RequestBody @Valid BookTransferRequest bookTransferRequest) {
        PaymentCommand command = webMapper.toCommand(bookTransferRequest, ApiVersion.V1);
        PaymentResult initiatedPayment = commandBus.send(command);
        return webMapper.toResponse(initiatedPayment);
    }

    @PostMapping("/v2/book-transfers/payment")
    public BookTransferResponse submitTransactionV2(@RequestBody @Valid BookTransferRequest bookTransferRequest) {
        PaymentCommand command = webMapper.toCommand(bookTransferRequest, ApiVersion.V2);
        PaymentResult initiatedPayment = commandBus.send(command);
        return webMapper.toResponse(initiatedPayment);
    }

    @PostMapping("/v3/book-transfers/payment")
    public BookTransferResponse submitTransactionV3(@RequestBody @Valid BookTransferRequestV3 bookTransferRequest) {
        HighValuePaymentCommand command = webMapper.toCommand(bookTransferRequest);
        PaymentResult initiatedPayment = commandBus.send(command);
        return webMapper.toResponse(initiatedPayment);
    }

    @PostMapping("/v3/compliance-cases/{caseId}/approve")
    public ResponseEntity<Void> approveComplianceCase(@PathVariable UUID caseId) {
        var command = new ApproveComplianceCaseCommand(caseId);
        commandBus.send(command);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/v3/compliance-cases/{caseId}/reject")
    public ResponseEntity<Void> rejectComplianceCase(@PathVariable UUID caseId, @RequestBody @Valid RejectComplianceRequest request) {
        var command = webMapper.toCommand(caseId, request);
        commandBus.send(command);
        return ResponseEntity.accepted().build();
    }


    @GetMapping({"/v1/book-transfers/payment/{id}", "/v2/book-transfers/payment/{id}"})
    public ResponseEntity<PaymentView> getTransactionInfo(@PathVariable UUID id) {
        return paymentQueryUseCase.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found for id: " + id));
    }

    @GetMapping({"/v1/book-transfers/payments", "/v2/book-transfers/payments"})
    public ResponseEntity<List<PaymentView>> findTransactionsByReference(@RequestParam("reference") String reference) {
        List<PaymentView> results = paymentQueryUseCase.findByReference(reference);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/v3/payments/{paymentId}/compliance-case")
    public ResponseEntity<ComplianceCaseView> getComplianceCaseByPaymentId(@PathVariable UUID paymentId) {
        return complianceQueryUseCase.findByPaymentId(paymentId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new TransactionNotFoundException("Compliance case not found for payment id: " + paymentId));
    }
}
