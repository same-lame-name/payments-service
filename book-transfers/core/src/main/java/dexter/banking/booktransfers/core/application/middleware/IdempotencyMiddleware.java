package dexter.banking.booktransfers.core.application.middleware;

import dexter.banking.booktransfers.core.domain.payment.exception.IdempotencyConflictException;
import dexter.banking.booktransfers.core.domain.shared.config.CommandProcessingContextHolder;
import dexter.banking.booktransfers.core.domain.shared.idempotency.IdempotencyData;
import dexter.banking.booktransfers.core.domain.shared.idempotency.IdempotencyStatus;
import dexter.banking.booktransfers.core.port.out.IdempotencyPort;
import dexter.banking.commandbus.Command;
import dexter.banking.commandbus.IdempotentCommand;
import dexter.banking.commandbus.Middleware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Order(2)
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyMiddleware implements Middleware {

    private final IdempotencyPort idempotencyPort;

    @Override
    @SuppressWarnings("unchecked")
    public <R, C extends Command<R>> R invoke(C command, Next<R> next) {
        // This middleware is now pure. It depends only on the core context and core ports.
        boolean isApplicable = CommandProcessingContextHolder.getContext()
                .map(ctx -> ctx.getJourneySpecification().isIdempotencyEnabled())
                .orElse(false);

        if (!isApplicable || !(command instanceof IdempotentCommand<?> idempotentCommand)) {
            return next.invoke();
        }

        UUID key = idempotentCommand.getIdempotencyKey();

        // Step 1: Attempt to acquire the lock. This is the hot path for new requests.
        if (idempotencyPort.tryAcquireLock(key)) {
            // Lock was acquired successfully. This is a new request.
            log.info("New idempotency key {}, lock acquired. Proceeding with new request.", key);
            try {
                R response = next.invoke();
                idempotencyPort.markCompleted(key, response);
                return response;
            } catch (Exception e) {
                idempotencyPort.releaseLock(key);
                throw e; // Rethrow the original exception
            }
        } else {
            // Lock was NOT acquired. Key already exists. Now, we find out why.
            log.debug("Idempotency key {} already exists. Fetching status...", key);
            IdempotencyData record = idempotencyPort.getOperationData(key)
                    .orElseThrow(() -> new IllegalStateException("FATAL: Lock failed but no record found for key: " + key)); // Should be impossible

            if (record.status() == IdempotencyStatus.COMPLETED) {
                // It's a duplicate request.
                log.info("Idempotency key {} already completed. Returning saved response.", key);
                return (R) record.response();
            } else { // status is STARTED
                // It's a concurrent request.
                log.warn("Idempotency key {} is already in progress.", key);
                throw new IdempotencyConflictException("Request with key " + key + " is already being processed.");
            }
        }
    }
}
