package dexter.banking.limit.pipeline.payee.middleware;

import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.stereotype.Component;

@Component
public class SchemeResolverMiddleware implements PipelineMiddleware<PayeeDto> {

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public <R> R process(PayeeDto payeeDto, Next<R> next) {
        if (payeeDto.getIban() != null && payeeDto.getIban().startsWith("DE")) {
            payeeDto.setDerivedScheme("SEPA");
        } else {
            payeeDto.setDerivedScheme("SWIFT");
        }

        return next.invoke();
    }
}