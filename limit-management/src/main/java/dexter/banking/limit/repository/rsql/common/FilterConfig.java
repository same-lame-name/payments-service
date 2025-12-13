package dexter.banking.limit.repository.rsql.common;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class FilterConfig<M> {

    private final Map<String, FilterableProperty<M>> properties = new HashMap<>();

    public FilterConfig<M> with(String apiName, M metadata, Class<?> type) {
        properties.put(apiName, new FilterableProperty<>(metadata, type));
        return this;
    }

    public FilterConfig<M> with(String apiName, M metadata) {
        // Default to String type if not specified
        properties.put(apiName, new FilterableProperty<>(metadata, String.class));
        return this;
    }

    public Optional<FilterableProperty<M>> getProperty(String apiName) {
        return Optional.ofNullable(properties.get(apiName));
    }
}