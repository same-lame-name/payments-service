package dexter.banking.limit.repository.rsql.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FilterableProperty<M> {
    private final M metadata;
    private final Class<?> type;
}