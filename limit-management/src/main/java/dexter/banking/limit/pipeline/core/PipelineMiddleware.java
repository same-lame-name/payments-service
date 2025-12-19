package dexter.banking.limit.pipeline.core;

import org.springframework.core.Ordered;

public interface PipelineMiddleware<T extends BaseRequest<?>> extends Ordered {
    
    <R> R process(T request, Next<R> next);

    @FunctionalInterface
    interface Next<R> {
        R invoke();
    }
}