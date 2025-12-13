package dexter.banking.limit.repository.rsql.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SortableProperty<M> {
    private final M metadata;
}