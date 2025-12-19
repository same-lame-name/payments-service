package dexter.banking.limit.pipeline.payee.middleware;

import dexter.banking.limit.config.repository.ConfigurationRepository;
import dexter.banking.limit.pipeline.core.BaseRequest;
import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfigLoaderMiddleware implements PipelineMiddleware<BaseRequest<?>> {

    private final ConfigurationRepository configRepo;

    @Override
    public <R> R process(BaseRequest<?> request, Next<R> next) {
        String scheme = request.getJourneyIdentifier();

        request.setServiceConfig(configRepo.findServiceConfig(scheme));
        request.setRulesConfig(configRepo.findRulesConfig(scheme));
        
        return next.invoke();
    }

    @Override
    public int getOrder() {
        return 2;
    }
}