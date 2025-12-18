package dexter.banking.limit.pipeline.payee.middleware;

import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.stereotype.Component;

@Component
public class SchemeResolverMiddleware implements PipelineMiddleware<PayeeDto> {

    @Override
    public <R> R process(PayeeDto request, Next<R> next) {
        //I will generate the ruleId and set it in the DTO
        // I will obtain values from the trhread-local and set it in the DTO
        if (request.getIban() != null && request.getIban().startsWith("DE")) {
            request.setDerivedScheme("SEPA");
        } else {
            request.setDerivedScheme("SWIFT");
        }
        
        return next.invoke();
    }

    @Override
    public int getOrder() {
        return 1;
    }
}