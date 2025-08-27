package dexter.banking.booktransfers.infrastructure.adapter.in.messaging;

import dexter.banking.booktransfers.core.domain.model.ProcessLimitEarmarkReversalResultCommand;
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
public class LimitEarmarkReversalListener {

    private final CommandBus commandBus;

    @JmsListener(destination = JmsConstants.LIMIT_MANAGEMENT_REVERSAL_RESPONSE)
    public void completeLimitEarmarkReversal(LimitManagementResponse result) {
        log.debug("Received Limit earmark reversal for txnId: {}. Translating to command.", result.getTransactionId());
        var command = new ProcessLimitEarmarkReversalResultCommand(result);
        command.execute(commandBus);
    }
}
