package dexter.banking.limit.repository.rsql.jpa;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.NoArgRSQLVisitorAdapter;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@RequiredArgsConstructor
public class JpaRsqlVisitor<T> extends NoArgRSQLVisitorAdapter<Specification<T>> {

    private final FilterConfig<String> filterConfig;

    @Override
    public Specification<T> visit(AndNode node) {
        return node.getChildren().stream()
                .map(child -> child.accept(this))
                .reduce(Specification::and)
                .orElse(null);
    }

    @Override
    public Specification<T> visit(OrNode node) {
        return node.getChildren().stream()
                .map(child -> child.accept(this))
                .reduce(Specification::or)
                .orElse(null);
    }

    @Override
    public Specification<T> visit(ComparisonNode node) {
        String apiName = node.getSelector();
        String jpaAttribute = filterConfig.getProperty(apiName)
                .orElseThrow(() -> new IllegalArgumentException("Unsupported filter field: " + apiName))
                .getMetadata();
        
        String value = node.getArguments().get(0);

        if (node.getOperator().equals(RSQLOperators.EQUAL)) {
            return (root, query, builder) -> builder.equal(root.get(jpaAttribute), value);
        }
        if (node.getOperator().equals(RSQLOperators.NOT_EQUAL)) {
            return (root, query, builder) -> builder.notEqual(root.get(jpaAttribute), value);
        }
        // This is where you would add support for other operators like GT, LT, etc.
        // by converting the 'value' string to the appropriate type (e.g., Integer, BigDecimal)
        // and using builder.greaterThan(), builder.lessThan(), etc.

        return null;
    }
}