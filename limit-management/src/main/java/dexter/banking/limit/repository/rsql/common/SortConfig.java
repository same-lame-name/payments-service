package dexter.banking.limit.repository.rsql.common;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Getter
public class SortConfig {

    private final Map<String, String> mapping = new HashMap<>();

    public SortConfig with(String apiName, String domainAttribute) {
        mapping.put(apiName, domainAttribute);
        return this;
    }

    public Optional<String> getDomainAttribute(String apiName) {
        return Optional.ofNullable(mapping.get(apiName));
    }
}