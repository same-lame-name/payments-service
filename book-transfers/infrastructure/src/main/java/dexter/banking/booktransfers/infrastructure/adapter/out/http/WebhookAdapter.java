package dexter.banking.booktransfers.infrastructure.adapter.out.http;

import dexter.banking.booktransfers.core.port.out.WebhookPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * An adapter for the WebhookPort that uses RestTemplate.
 * This is implemented as a class because it needs to handle dynamic URLs,
 * which is simpler with RestTemplate than with Feign.
 */
@Slf4j
@Component
class WebhookAdapter implements WebhookPort {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void notifyTransactionComplete(String webhookUrl, Object status) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            log.debug("No webhook URL provided, skipping notification.");
            return;
        }

        try {
            log.info("Sending webhook notification to {} with status {}", webhookUrl, status);
            restTemplate.postForObject(webhookUrl, status, String.class);
        } catch (Exception e) {
            // In a real application, this should probably go to a retry queue.
            log.error("Error calling webhook URL: {}", webhookUrl, e);
        }
    }
}

