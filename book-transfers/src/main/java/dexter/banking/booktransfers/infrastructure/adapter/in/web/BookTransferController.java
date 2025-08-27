package dexter.banking.booktransfers.infrastructure.adapter.in.web;

import dexter.banking.booktransfers.core.domain.exception.TransactionNotFoundException;
import dexter.banking.booktransfers.core.domain.model.ApiVersion;
import dexter.banking.booktransfers.core.domain.model.PaymentCommand;
import dexter.banking.booktransfers.core.domain.model.PaymentResponse;
import dexter.banking.booktransfers.core.usecase.payment.query.PaymentQueryUseCase;
import dexter.banking.booktransfers.core.usecase.payment.query.PaymentView;
import dexter.banking.booktransfers.infrastructure.adapter.in.web.dto.BookTransferRequest;
import dexter.banking.booktransfers.infrastructure.adapter.in.web.dto.BookTransferResponse;
import dexter.banking.booktransfers.infrastructure.adapter.in.web.mapper.WebMapper;
import dexter.banking.commandbus.CommandBus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
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
public class BookTransferController {

    private final CommandBus commandBus;
    private final PaymentQueryUseCase paymentQueryUseCase;
    private final WebMapper webMapper;

    @PostMapping("/v1/book-transfers/payment")
    public BookTransferResponse submitTransactionV1(@RequestBody @Valid BookTransferRequest bookTransferRequest) {
        PaymentCommand command = webMapper.toCommand(bookTransferRequest, ApiVersion.V1);
        PaymentResponse initiatedPaymentResponse = command.execute(commandBus);
        return webMapper.toResponse(initiatedPaymentResponse);
    }

    @PostMapping("/v2/book-transfers/payment")
    public BookTransferResponse submitTransactionV2(@RequestBody @Valid BookTransferRequest bookTransferRequest) {
        PaymentCommand command = webMapper.toCommand(bookTransferRequest, ApiVersion.V2);
        PaymentResponse initiatedPaymentResponse = command.execute(commandBus);
        return webMapper.toResponse(initiatedPaymentResponse);
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
}
