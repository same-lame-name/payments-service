package dexter.banking.limit.repository.rsql.jpa.builder;

import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.LogicalOperator;
import cz.jirutka.rsql.parser.ast.Node;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class RSQLSpecificationBuilder<T> {

    public Specification<T> build(Node node) {
        if (node instanceof LogicalNode) {
            return build((LogicalNode) node);
        }
        if (node instanceof ComparisonNode) {
            return build((ComparisonNode) node);
        }
        return null;
    }

    public Specification<T> build(LogicalNode logicalNode) {
        List<Specification<T>> specs = logicalNode.getChildren()
                .stream()
                .map(this::build)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (specs.isEmpty()) {
            return null;
        }

        if (logicalNode.getOperator() == LogicalOperator.AND) {
            return specs.stream().reduce(Specification::and).orElse(null);
        } else if (logicalNode.getOperator() == LogicalOperator.OR) {
            return specs.stream().reduce(Specification::or).orElse(null);
        }

        return specs.get(0);
    }

    public Specification<T> build(ComparisonNode comparisonNode) {
        return Specification.where(
                new RSQLSpecification<T>(
                        comparisonNode.getSelector(),
                        comparisonNode.getOperator(),
                        comparisonNode.getArguments()
                )
        );
    }
}