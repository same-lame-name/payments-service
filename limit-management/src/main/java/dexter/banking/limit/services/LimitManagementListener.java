package dexter.banking.limit.services;

import dexter.banking.model.JmsConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import dexter.banking.model.LimitManagementRequest;
import dexter.banking.model.LimitManagementResponse;

@Slf4j
@RequiredArgsConstructor
@Component
public class LimitManagementListener {

    private final LimitManagementService limitManagementService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConstants.LIMIT_MANAGEMENT_REQUEST)
    public void listen(LimitManagementRequest limitManagementRequest) {
        log.debug("Limit management request (JMS): {}", limitManagementRequest);
        LimitManagementResponse limitManagementResult = limitManagementService.submitLimitManagementRequest(limitManagementRequest);
        jmsTemplate.convertAndSend(JmsConstants.LIMIT_MANAGEMENT_RESPONSE, limitManagementResult);
    }
}
