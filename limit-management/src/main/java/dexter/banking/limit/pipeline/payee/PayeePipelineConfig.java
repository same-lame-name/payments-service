package dexter.banking.limit.pipeline.payee;

import dexter.banking.limit.pipeline.core.ExecutionStrategy;
import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.pipeline.core.PipelineOrchestrator;
import dexter.banking.limit.pipeline.core.StrategyRegistry;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PayeePipelineConfig {

    @Bean
    public StrategyRegistry<PayeeDto, PayeeDto> payeeStrategyRegistry(List<ExecutionStrategy<PayeeDto, PayeeDto>> strategies) {
        return new StrategyRegistry<>(strategies);
    }

    @Bean
    public PipelineOrchestrator<PayeeDto, PayeeDto> payeePipelineOrchestrator(
            List<PipelineMiddleware<PayeeDto>> pipeline,
            StrategyRegistry<PayeeDto, PayeeDto> registry) {
        return new PipelineOrchestrator<>(pipeline, registry);
    }
}