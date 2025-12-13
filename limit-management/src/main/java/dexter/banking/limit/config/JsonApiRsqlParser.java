package dexter.banking.limit.config;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.Node;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
public class JsonApiRsqlParser {

    // Regex to capture: 1=attribute, 2=operator (optional), 3=value
    private static final Pattern FILTER_PATTERN = Pattern.compile(JsonApiConstants.FILTER + "\\[([a-zA-Z0-9_]+)](\\[([a-zA-Z]+)])?");

    public Node parse(Map<String, String> params) {
        List<Node> nodes = params.entrySet().stream()
                .map(entry -> {
                    Matcher matcher = FILTER_PATTERN.matcher(entry.getKey());
                    if (matcher.matches()) {
                        String attribute = matcher.group(1);
                        String jsonApiOp = matcher.group(3); // Can be null for simple equality
                        String value = entry.getValue();

                        JsonApiConstants.JsonApiOperator operator = (jsonApiOp != null)
                                ? JsonApiConstants.JsonApiOperator.fromString(jsonApiOp)
                                : JsonApiConstants.JsonApiOperator.EQUALS;

                        if (operator != null) {
                            return new ComparisonNode(operator.getRsqlOp(), attribute, List.of(value));
                        }
                    }
                    return null;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());

        if (nodes.isEmpty()) {
            return null;
        }
        if (nodes.size() == 1) {
            return nodes.get(0);
        }
        return new AndNode(nodes);
    }
}