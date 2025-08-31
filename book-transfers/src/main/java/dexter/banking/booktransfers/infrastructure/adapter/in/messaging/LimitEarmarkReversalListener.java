package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.usecase.payment.orchestration.async.AsyncPaymentV2CommandHandler;
import dexter.banking.booktransfers.infrastructure.adapter.in.messaging.mapper.MessagingAdapterMapper;
import dexter.banking.model.JmsConstants;
import dexter.banking.model.LimitManagementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LimitEarmarkReversalListener {

    private final AsyncPaymentV2CommandHandler orchestrator;
    private final MessagingAdapterMapper mapper;

    @JmsListener(destination = JmsConstants.LIMIT_MANAGEMENT_REVERSAL_RESPONSE)
    public void completeLimitEarmarkReversal(LimitManagementResponse result) {
        log.debug("Received Limit earmark reversal DTO for txnId: {}. Delegating to orchestrator.", result.getTransactionId());
        var domainResult = mapper.toReversalDomain(result);
        orchestrator.processLimitReversalResult(domainResult, result.getTransactionId());
    }
}
