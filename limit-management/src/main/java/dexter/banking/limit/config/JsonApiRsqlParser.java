package dexter.banking.limit.config;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

@Component
public class JsonApiRsqlParser {

    private final RSQLParser rsqlParser = new RSQLParser();

    public Node parse(Map<String, String> params) {
        // Filter map for keys starting with "filter" and join them with RSQL AND (;)
        String rsqlString = params.entrySet().stream()
                .filter(e -> e.getKey().startsWith("filter"))
                .map(this::toRsqlFragment)
                .collect(Collectors.joining(";"));

        if (rsqlString == null || rsqlString.isBlank()) {
            return null;
        }
        return rsqlParser.parse(rsqlString);
    }

    private String toRsqlFragment(Map.Entry<String, String> entry) {
        String key = entry.getKey();
        String value = entry.getValue();

        // Parse: filter[amount][gt] -> amount
        int firstBracket = key.indexOf('[');
        int secondBracket = key.indexOf(']');
        String field = key.substring(firstBracket + 1, secondBracket);

        // Parse Operator: [gt]
        String op = "=="; // Default
        int thirdBracket = key.indexOf('[', secondBracket + 1);
        if (thirdBracket > 0) {
            int fourthBracket = key.indexOf(']', thirdBracket + 1);
            String opCode = key.substring(thirdBracket + 1, fourthBracket);
            op = mapOperator(opCode);
        }

        // Quote value to handle spaces safely in RSQL
        String safeValue = value.contains(" ") ? "'" + value + "'" : value;

        return field + op + safeValue;
    }

    private String mapOperator(String jsonApiOp) {
        return switch (jsonApiOp) {
            case "gt" -> RSQLOperators.GREATER_THAN.getSymbol();
            case "lt" -> RSQLOperators.LESS_THAN.getSymbol();
            case "ge" -> RSQLOperators.GREATER_THAN_OR_EQUAL.getSymbol();
            case "le" -> RSQLOperators.LESS_THAN_OR_EQUAL.getSymbol();
            case "neq" -> RSQLOperators.NOT_EQUAL.getSymbol();
            case "like" -> RSQLOperators.EQUAL.getSymbol(); // Wildcard handled by value content usually
            default -> RSQLOperators.EQUAL.getSymbol();
        };
    }
}