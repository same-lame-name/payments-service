package dexter.banking.limit.repository.rsql.jpa;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.NoArgRSQLVisitorAdapter;
import cz.jirutka.rsql.parser.ast.OrNode;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

@RequiredArgsConstructor
public class JpaSpecificationVisitor<T> extends NoArgRSQLVisitorAdapter<Specification<T>> {

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

        return new RSQLSpecification<>(
                jpaAttribute,
                node.getOperator(),
                node.getArguments()
        );
    }
}