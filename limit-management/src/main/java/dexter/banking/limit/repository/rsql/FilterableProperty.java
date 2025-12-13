package dexter.banking.limit.repository.rsql;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

@Getter
@RequiredArgsConstructor
public class FilterableProperty<T, R> {
    private final Function<T, R> extractor;
    private final Class<R> type;
}