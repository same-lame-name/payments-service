package dexter.banking.limit.pipeline.payee.middleware;

import dexter.banking.limit.config.model.ServiceConfig;
import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class StructuralValidatorMiddleware implements PipelineMiddleware<PayeeDto> {

    @Override
    public void process(PayeeDto request) {
        ServiceConfig config = request.getServiceConfig();

        if (config.ibanValidationEnabled()) {
            String regex = config.validationRegex();
            if (request.getIban() == null || !request.getIban().matches(regex)) {
                throw new IllegalArgumentException("Invalid IBAN format for scheme: " + request.getJourneyIdentifier());
            }
        }
    }

    @Override
    public int getOrder() {
        return 4;
    }
}