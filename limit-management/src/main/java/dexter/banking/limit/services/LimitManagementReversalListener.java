package dexter.banking.limit.services;

import dexter.banking.model.JmsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import dexter.banking.model.LimitManagementReversalRequest;
import dexter.banking.model.LimitManagementResponse;

@Slf4j
@RequiredArgsConstructor
@Component
public class LimitManagementReversalListener {

    private final LimitManagementService limitManagementService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConstants.LIMIT_MANAGEMENT_REVERSAL_REQUEST)
    public void listen(LimitManagementReversalRequest limitManagementReversalRequest) {
        log.debug("Limit management reversal request (JMS): {}", limitManagementReversalRequest);
        LimitManagementResponse limitManagementReversalResult = limitManagementService.limitManagementReversalRequest(limitManagementReversalRequest);
        jmsTemplate.convertAndSend(JmsConstants.LIMIT_MANAGEMENT_REVERSAL_RESPONSE, limitManagementReversalResult);
    }
}
