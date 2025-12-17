package dexter.banking.limit.pipeline.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StrategyRegistry<T extends BaseRequest<T>, R> {

    private final Map<String, ExecutionStrategy<T, R>> registry = new HashMap<>();

    public StrategyRegistry(List<ExecutionStrategy<T, R>> strategies) {
        for (ExecutionStrategy<T, R> strategy : strategies) {
            Set<String> keys = strategy.getSupportedIdentifiers();
            for (String key : keys) {
                // 1. Fail Fast on Collision
                if (registry.containsKey(key)) {
                    ExecutionStrategy<T, R> existing = registry.get(key);
                    throw new IllegalStateException(String.format(
                            "Startup Fatal Error: Duplicate Key '%s' detected! Claimed by both %s and %s",
                            key, existing.getClass().getSimpleName(), strategy.getClass().getSimpleName()
                    ));
                }
                // 2. Register
                registry.put(key, strategy);
            }
        }
    }

    public ExecutionStrategy<T, R> getStrategy(String key) {
        ExecutionStrategy<T, R> strategy = registry.get(key);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for key: " + key);
        }
        return strategy;
    }
}