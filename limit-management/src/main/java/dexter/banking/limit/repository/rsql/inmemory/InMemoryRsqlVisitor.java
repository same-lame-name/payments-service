package dexter.banking.limit.repository.rsql.inmemory;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.NoArgRSQLVisitorAdapter;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import dexter.banking.limit.repository.rsql.common.FilterableProperty;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Predicate<T> visit(ComparisonNode node) {
        FilterableProperty<Function<T, ?>> property = filterConfig.getProperty(node.getSelector())
                .orElseThrow(() -> new IllegalArgumentException("Unsupported filter field: " + node.getSelector()));

        List<Object> arguments = node.getArguments().stream()
                .map(arg -> convert(arg, property.getType()))
                .collect(Collectors.toList());
        
        Object argument = arguments.get(0);

        return entity -> {
            Object entityValue = property.getMetadata().apply(entity);
            if (entityValue == null) {
                return false;
            }

            if (node.getOperator().equals(RSQLOperators.EQUAL)) {
                return entityValue.equals(argument);
            }
            if (node.getOperator().equals(RSQLOperators.NOT_EQUAL)) {
                return !entityValue.equals(argument);
            }
            if (node.getOperator().equals(RSQLOperators.IN)) {
                return arguments.contains(entityValue);
            }
            if (node.getOperator().equals(RSQLOperators.GREATER_THAN)) {
                return ((Comparable) entityValue).compareTo(argument) > 0;
            }
            if (node.getOperator().equals(RSQLOperators.GREATER_THAN_OR_EQUAL)) {
                return ((Comparable) entityValue).compareTo(argument) >= 0;
            }
            if (node.getOperator().equals(RSQLOperators.LESS_THAN)) {
                return ((Comparable) entityValue).compareTo(argument) < 0;
            }
            if (node.getOperator().equals(RSQLOperators.LESS_THAN_OR_EQUAL)) {
                return ((Comparable) entityValue).compareTo(argument) <= 0;
            }
            
            return false;
        };
    }

    private Object convert(String value, Class<?> type) {
        if (type.equals(String.class)) {
            return value;
        } else if (type.equals(Integer.class) || type.equals(int.class)) {
            return Integer.parseInt(value);
        } else if (type.equals(Long.class) || type.equals(long.class)) {
            return Long.parseLong(value);
        } else if (type.equals(Double.class) || type.equals(double.class)) {
            return Double.parseDouble(value);
        } else if (type.equals(BigDecimal.class)) {
            return new BigDecimal(value);
        } else if (type.equals(LocalDate.class)) {
            return LocalDate.parse(value);
        } else if (type.equals(LocalDateTime.class)) {
            return LocalDateTime.parse(value);
        } else if (type.equals(OffsetDateTime.class)) {
            return OffsetDateTime.parse(value);
        } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return Boolean.parseBoolean(value);
        }
        throw new IllegalArgumentException("Unsupported property type: " + type.getName());
    }
}