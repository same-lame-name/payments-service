package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.usecase.event.ProcessLimitEarmarkResultCommand;
import dexter.banking.booktransfers.infrastructure.adapter.in.messaging.mapper.MessagingAdapterMapper;
import dexter.banking.commandbus.CommandBus;
import dexter.banking.model.JmsConstants;
import dexter.banking.model.LimitManagementResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LimitEarmarkListener {

    private final CommandBus commandBus;
    private final MessagingAdapterMapper mapper;

    @JmsListener(destination = JmsConstants.LIMIT_MANAGEMENT_RESPONSE)
    public void completeLimitEarmark(LimitManagementResponse result) {
        log.debug("Received Limit earmark Result DTO for txnId: {}. Translating to command.", result.getTransactionId());
        var domainResult = mapper.toDomain(result);
        var command = new ProcessLimitEarmarkResultCommand(result.getTransactionId(), domainResult);
        command.execute(commandBus);
    }
}
