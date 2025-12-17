package dexter.banking.limit.pipeline.core;

import java.util.Set;

public interface ExecutionStrategy<T extends BaseRequest<T>, R> {
    Set<String> getSupportedIdentifiers();
    R execute(T request);
}