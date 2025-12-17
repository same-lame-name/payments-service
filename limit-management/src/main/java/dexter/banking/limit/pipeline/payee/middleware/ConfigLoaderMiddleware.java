package dexter.banking.limit.pipeline.payee.middleware;

import dexter.banking.limit.config.repository.ConfigurationRepository;
import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.web.dto.PayeeDto;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfigLoaderMiddleware implements PipelineMiddleware<PayeeDto> {

    private final ConfigurationRepository configRepo;

    @Override
    public void process(PayeeDto request) {
        String scheme = request.getJourneyIdentifier();

        request.setServiceConfig(configRepo.findServiceConfig(scheme));
        request.setRulesConfig(configRepo.findRulesConfig(scheme));
    }

    @Override
    public int getOrder() {
        return 2;
    }
}