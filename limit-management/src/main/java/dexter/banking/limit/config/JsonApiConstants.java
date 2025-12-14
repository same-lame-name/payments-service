package dexter.banking.limit.config;

import cz.jirutka.rsql.parser.ast.RSQLOperators;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class JsonApiConstants {

    private JsonApiConstants() {
    }

    public static final String MEDIA_TYPE = "application/vnd.api+json";
    public static final String FILTER = "filter";
    public static final String PAGE = "page";
    public static final String NUMBER = "number";
    public static final String SIZE = "size";
    public static final String PAGE_NUMBER = "page[number]";
    public static final String PAGE_NUMBER_ENCODED = "page%5Bnumber%5D";
    public static final String PAGE_SIZE = "page[size]";
    public static final String PAGE_SIZE_ENCODED = "page%5Bsize%5D";
    public static final String SORT = "sort";

    @Getter
    @RequiredArgsConstructor
    public enum JsonApiOperator {
        GREATER_THAN("gt", RSQLOperators.GREATER_THAN),
        LESS_THAN("lt", RSQLOperators.LESS_THAN),
        GREATER_THAN_OR_EQUAL("ge", RSQLOperators.GREATER_THAN_OR_EQUAL),
        LESS_THAN_OR_EQUAL("le", RSQLOperators.LESS_THAN_OR_EQUAL),
        NOT_EQUAL("neq", RSQLOperators.NOT_EQUAL),
        IN("in", RSQLOperators.IN),
        EQUALS("eq", RSQLOperators.EQUAL);

        private final String jsonApiOp;
        private final cz.jirutka.rsql.parser.ast.ComparisonOperator rsqlOp;

        private static final Map<String, JsonApiOperator> opMap =
                Arrays.stream(values()).collect(Collectors.toMap(JsonApiOperator::getJsonApiOp, Function.identity()));

        public static JsonApiOperator fromString(String text) {
            JsonApiOperator op = opMap.get(text);
            if (op == null) {
                throw new IllegalArgumentException("Unsupported JSON:API operator: " + text);
            }

            return op;
        }
    }
}