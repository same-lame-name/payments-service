package dexter.banking.limit.repository.rsql;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Getter
public class FilterConfig<T> {

    private final Map<String, FilterableProperty<T, ?>> properties = new HashMap<>();

    public <R> FilterConfig<T> with(String apiName, Function<T, R> extractor, Class<R> type) {
        properties.put(apiName, new FilterableProperty<>(extractor, type));
        return this;
    }

    public <R> FilterConfig<T> with(String apiName, Function<T, R> extractor) {
        // Default to String type if not specified
        @SuppressWarnings("unchecked")
        Class<R> type = (Class<R>) String.class;
        properties.put(apiName, new FilterableProperty<>(extractor, type));
        return this;
    }

    public Optional<FilterableProperty<T, ?>> getProperty(String apiName) {
        return Optional.ofNullable(properties.get(apiName));
    }
}