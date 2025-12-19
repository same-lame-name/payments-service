package dexter.banking.limit.pipeline.core;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PipelineOrchestrator<T extends BaseRequest<T>, R> {

    private final List<PipelineMiddleware<? super T>> pipeline;
    private final StrategyRegistry<T, R> registry;

    public R handle(T request) {
        // The final action in the chain is to execute the business strategy
        PipelineMiddleware.Next<R> finalAction = () -> {
            String key = request.getJourneyIdentifier();
            ExecutionStrategy<T, R> strategy = registry.getStrategy(key);
            return strategy.execute(request);
        };

        // Build the chain backwards from the final action
        PipelineMiddleware.Next<R> chain = finalAction;
        for (int i = pipeline.size() - 1; i >= 0; i--) {
            PipelineMiddleware<? super T> currentMiddleware = pipeline.get(i);
            PipelineMiddleware.Next<R> nextInChain = chain; // Capture current chain link
            chain = () -> currentMiddleware.process(request, nextInChain);
        }

        // Invoke the fully constructed chain from the beginning
        return chain.invoke();
    }
}