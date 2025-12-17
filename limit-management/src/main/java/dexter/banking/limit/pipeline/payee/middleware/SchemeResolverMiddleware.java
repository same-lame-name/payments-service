package dexter.banking.limit.pipeline.payee.middleware;

import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
public class SchemeResolverMiddleware implements PipelineMiddleware<PayeeDto> {

    @Override
    public void process(PayeeDto request) {
        if (request.getIban() != null && request.getIban().startsWith("DE")) {
            request.setDerivedScheme("SEPA");
        } else {
            request.setDerivedScheme("SWIFT");
        }
    }

    @Override
    public int getOrder() {
        return 1;
    }
}