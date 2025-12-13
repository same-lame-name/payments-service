package dexter.banking.limit.repository.rsql.jpa;

import dexter.banking.limit.repository.rsql.common.SortConfig;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SortTranslator {

    public Sort translate(Sort input, SortConfig<String> config) {
        if (input.isUnsorted()) {
            return input;
        }

        List<Sort.Order> translatedOrders = input.stream()
                .map(order -> {
                    String domainAttribute = config.getProperty(order.getProperty())
                            .orElseThrow(() -> new IllegalArgumentException("Unsupported sort field: " + order.getProperty()))
                            .getMetadata();
                    return new Sort.Order(order.getDirection(), domainAttribute);
                })
                .collect(Collectors.toList());

        return Sort.by(translatedOrders);
    }
}