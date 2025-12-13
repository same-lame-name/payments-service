package dexter.banking.limit.repository.rsql.common;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class SortConfig<M> {

    private final Map<String, SortableProperty<M>> mapping = new HashMap<>();

    public SortConfig<M> with(String apiName, M metadata) {
        mapping.put(apiName, new SortableProperty<>(metadata));
        return this;
    }

    public Optional<SortableProperty<M>> getProperty(String apiName) {
        return Optional.ofNullable(mapping.get(apiName));
    }
}