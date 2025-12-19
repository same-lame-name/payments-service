package dexter.banking.limit.pipeline.payee;

import dexter.banking.limit.pipeline.core.BaseRequest;
import dexter.banking.limit.pipeline.core.ExecutionStrategy;
import dexter.banking.limit.pipeline.core.PipelineMiddleware;
import dexter.banking.limit.pipeline.core.PipelineOrchestrator;
import dexter.banking.limit.pipeline.core.StrategyRegistry;
import dexter.banking.limit.web.dto.PayeeDto;
import dexter.banking.limit.web.dto.UpdatePayeePatch;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PayeePipelineConfig {

    @Bean
    public PipelineOrchestrator<UpdatePayeePatch, PayeeDto> createPayeeOrchestrator(
            List<PipelineMiddleware<? super UpdatePayeePatch>> middlewares,
            StrategyRegistry<UpdatePayeePatch, PayeeDto> registry) {
        return new PipelineOrchestrator<>(middlewares, registry);
    }

    @Bean
    public PipelineOrchestrator<PayeeDto, PayeeDto> updatePayeeOrchestrator(
            List<PipelineMiddleware<? super PayeeDto>> middlewares,
            StrategyRegistry<PayeeDto, PayeeDto> registry) {
        return new PipelineOrchestrator<>(middlewares, registry);
    }

    @Bean
    public StrategyRegistry<PayeeDto, PayeeDto> payeeStrategyRegistry(
            List<ExecutionStrategy<PayeeDto, PayeeDto>> strategies) {
        return new StrategyRegistry<>(strategies);
    }

    @Bean
    public StrategyRegistry<UpdatePayeePatch, PayeeDto> updatePayeeStrategyRegistry(
            List<ExecutionStrategy<UpdatePayeePatch, PayeeDto>> strategies) {
        return new StrategyRegistry<>(strategies);
    }
}