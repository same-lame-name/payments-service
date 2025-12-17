package dexter.banking.limit.pipeline.core;

import org.springframework.core.Ordered;

public interface PipelineMiddleware<T extends BaseRequest<T>> extends Ordered {
    void process(T request);
}