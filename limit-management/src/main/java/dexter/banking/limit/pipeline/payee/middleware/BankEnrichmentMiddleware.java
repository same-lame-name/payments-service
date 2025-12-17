package dexter.banking.limit.pipeline.payee.middleware;

import dexter.banking.limit.config.model.ServiceConfig;
import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.web.dto.BankDetails;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class BankEnrichmentMiddleware implements PipelineMiddleware<PayeeDto> {

    @Override
    public void process(PayeeDto request) {
        ServiceConfig config = request.getServiceConfig();

        if (config.bankEnrichmentEnabled()) {
            // Mock enrichment logic
            String iban = request.getIban();
            String bic = iban.substring(0, 4) + "XXXX"; // Dummy logic
            request.setEnrichedBankDetails(new BankDetails(bic, "Mock Bank " + bic));
        }
    }

    @Override
    public int getOrder() {
        return 3;
    }
}