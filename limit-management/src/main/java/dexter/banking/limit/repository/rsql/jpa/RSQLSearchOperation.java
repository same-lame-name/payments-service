package dexter.banking.limit.repository.rsql.jpa;

import cz.jirutka.rsql.parser.ast.ComparisonOperator;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@AllArgsConstructor
@Getter
public enum RSQLSearchOperation {

    EQUAL(RSQLOperators.EQUAL),
    NOT_EQUAL(RSQLOperators.NOT_EQUAL),
    GREATER_THAN(RSQLOperators.GREATER_THAN),
    GREATER_THAN_OR_EQUAL(RSQLOperators.GREATER_THAN_OR_EQUAL),
    LESS_THAN(RSQLOperators.LESS_THAN),
    LESS_THAN_OR_EQUAL(RSQLOperators.LESS_THAN_OR_EQUAL),
    IN(RSQLOperators.IN),
    NOT_IN(RSQLOperators.NOT_IN);

    private final ComparisonOperator operator;

    private static final Map<ComparisonOperator, RSQLSearchOperation> OPERATOR_MAP =
            Arrays.stream(values()).collect(Collectors.toMap(RSQLSearchOperation::getOperator, Function.identity()));

    public static RSQLSearchOperation getSimpleOperator(ComparisonOperator operator) {
        RSQLSearchOperation operation = OPERATOR_MAP.get(operator);
        if (operation == null) {
            throw new IllegalArgumentException("Unsupported operator: " + operator.getSymbol());
        }
        return operation;
    }
}