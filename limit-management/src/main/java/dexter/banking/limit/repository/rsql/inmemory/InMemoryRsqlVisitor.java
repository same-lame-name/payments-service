package dexter.banking.limit.repository.rsql.inmemory;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.NoArgRSQLVisitorAdapter;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import dexter.banking.limit.repository.rsql.common.FilterableProperty;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;
import java.util.function.Predicate;

@RequiredArgsConstructor
public class InMemoryRsqlVisitor<T> extends NoArgRSQLVisitorAdapter<Predicate<T>> {

    private final FilterConfig<Function<T, ?>> filterConfig;

    @Override
    public Predicate<T> visit(AndNode node) {
        return node.getChildren().stream()
                .map(child -> child.accept(this))
                .reduce(Predicate::and)
                .orElse(x -> true);
    }

    @Override
    public Predicate<T> visit(OrNode node) {
        return node.getChildren().stream()
                .map(child -> child.accept(this))
                .reduce(Predicate::or)
                .orElse(x -> false);
    }

    @Override
    public Predicate<T> visit(ComparisonNode node) {
        FilterableProperty<Function<T, ?>> property = filterConfig.getProperty(node.getSelector())
                .orElseThrow(() -> new IllegalArgumentException("Unsupported filter field: " + node.getSelector()));

        Object argument = convert(node.getArguments().get(0), property.getType());

        return entity -> {
            Object entityValue = property.getMetadata().apply(entity);

            if (node.getOperator().equals(RSQLOperators.EQUAL)) {
                return entityValue != null && entityValue.equals(argument);
            }
            if (node.getOperator().equals(RSQLOperators.NOT_EQUAL)) {
                return entityValue != null && !entityValue.equals(argument);
            }
            // Add more operators here (GT, LT, etc.) for numeric/date types
            
            return false;
        };
    }

    private Object convert(String value, Class<?> type) {
        if (type.equals(String.class)) {
            return value;
        }
        if (type.equals(Integer.class) || type.equals(int.class)) {
            return Integer.parseInt(value);
        }
        // Add more type conversions as needed (Long, BigDecimal, LocalDate, etc.)
        throw new IllegalArgumentException("Unsupported property type: " + type.getName());
    }
}