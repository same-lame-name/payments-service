package dexter.banking.booktransfers.infrastructure.adapter.in.web;

import dexter.banking.booktransfers.core.domain.payment.exception.IdempotencyConflictException;
import dexter.banking.booktransfers.core.domain.payment.exception.TransactionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import java.time.OffsetDateTime;

@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFoundException(TransactionNotFoundException ex, WebRequest request) {
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error(HttpStatus.NOT_FOUND.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleIdempotencyConflictException(IdempotencyConflictException ex, WebRequest request) {
        ApiErrorResponse errorResponse = ApiErrorResponse.builder()
                .timestamp(OffsetDateTime.now())
                .status(HttpStatus.CONFLICT.value())
                .error(HttpStatus.CONFLICT.getReasonPhrase())
                .message(ex.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
}
