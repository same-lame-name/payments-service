package dexter.banking.limit.pipeline.core;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PipelineOrchestrator<T extends BaseRequest<T>, R> {

    private final List<PipelineMiddleware<T>> pipeline;
    private final StrategyRegistry<T, R> registry;

    public R handle(T request) {
        // 1. Run Pipeline (Pre-Processing)
        for (PipelineMiddleware<T> step : pipeline) {
            step.process(request);
        }

        // 2. Resolve Strategy
        String key = request.getJourneyIdentifier();
        ExecutionStrategy<T, R> strategy = registry.getStrategy(key);

        // 3. Execute
        return strategy.execute(request);
    }
}