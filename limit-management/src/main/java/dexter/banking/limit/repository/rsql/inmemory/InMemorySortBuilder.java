package dexter.banking.limit.repository.rsql.inmemory;

import dexter.banking.limit.repository.rsql.common.SortConfig;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.function.Function;

@Component
public class InMemorySortBuilder {

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Comparator<T> build(Sort sort, SortConfig<Function<T, ? extends Comparable>> config) {
        if (sort.isUnsorted()) {
            return null;
        }

        Comparator<T> comparator = null;
        for (Sort.Order order : sort) {
            Function<T, ? extends Comparable> extractor = config.getProperty(order.getProperty())
                    .orElseThrow(() -> new IllegalArgumentException("Unsupported sort field: " + order.getProperty()))
                    .getMetadata();

            Comparator<T> current = Comparator.comparing(extractor);
            if (order.isDescending()) {
                current = current.reversed();
            }

            comparator = (comparator == null) ? current : comparator.thenComparing(current);
        }
        return comparator;
    }
}