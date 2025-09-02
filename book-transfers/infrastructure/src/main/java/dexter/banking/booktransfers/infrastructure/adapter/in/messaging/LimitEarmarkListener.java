package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.application.payment.command.AsyncPaymentV2CommandHandler;
import dexter.banking.model.JmsConstants;
import dexter.banking.model.LimitManagementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class LimitEarmarkListener {

    private final AsyncPaymentV2CommandHandler orchestrator;
    private final MessagingAdapterMapper mapper;

    @JmsListener(destination = JmsConstants.LIMIT_MANAGEMENT_RESPONSE)
    public void completeLimitEarmark(LimitManagementResponse result) {
        log.debug("Received Limit earmark Result DTO for txnId: {}. Delegating to orchestrator.", result.getTransactionId());
        var domainResult = mapper.toDomain(result);
        orchestrator.processLimitEarmarkResult(domainResult, result.getTransactionId());
    }
}
