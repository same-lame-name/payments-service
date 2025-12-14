package dexter.banking.limit.repository.rsql.jpa.builder;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class RSQLSpecification<T> implements Specification<T> {

    private final String property;
    private final ComparisonOperator operator;
    private final List<String> arguments;

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Path path = resolvePath(root, property);
        List<Object> args = castArguments(path);
        Object argument = args.get(0);

        switch (RSQLSearchOperation.getSimpleOperator(operator)) {
            case EQUAL:
                return builder.equal(path, argument);
            case NOT_EQUAL:
                return builder.notEqual(path, argument);
            case GREATER_THAN:
                return builder.greaterThan(path, (Comparable) argument);
            case GREATER_THAN_OR_EQUAL:
                return builder.greaterThanOrEqualTo(path, (Comparable) argument);
            case LESS_THAN:
                return builder.lessThan(path, (Comparable) argument);
            case LESS_THAN_OR_EQUAL:
                return builder.lessThanOrEqualTo(path, (Comparable) argument);
            case IN:
                return path.in(args);
            case NOT_IN:
                return builder.not(path.in(args));
        }

        return null;
    }

    private Path<?> resolvePath(Root<T> root, String attribute) {
        String[] parts = attribute.split("\\.");
        Path<?> path = root;
        for (String part : parts) {
            path = path.get(part);
        }
        return path;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private List<Object> castArguments(Path<?> path) {
        Class<?> type = path.getJavaType();
        return arguments.stream().map(arg -> {
            if (type.isEnum()) {
                return Enum.valueOf((Class<Enum>) type, arg);
            } else if (type.equals(Integer.class) || type.equals(int.class)) {
                return Integer.parseInt(arg);
            } else if (type.equals(Long.class) || type.equals(long.class)) {
                return Long.parseLong(arg);
            } else if (type.equals(Double.class) || type.equals(double.class)) {
                return Double.parseDouble(arg);
            } else if (type.equals(BigDecimal.class)) {
                return new BigDecimal(arg);
            } else if (type.equals(LocalDate.class)) {
                return LocalDate.parse(arg);
            } else if (type.equals(LocalDateTime.class)) {
                return LocalDateTime.parse(arg);
            } else if (type.equals(Boolean.class) || type.equals(boolean.class)) {
                return Boolean.parseBoolean(arg);
            } else {
                return arg;
            }
        }).collect(Collectors.toList());
    }
}