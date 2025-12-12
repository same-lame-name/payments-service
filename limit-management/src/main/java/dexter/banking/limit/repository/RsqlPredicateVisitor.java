package dexter.banking.limit.repository;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.NoArgRSQLVisitorAdapter;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLOperators;
import dexter.banking.limit.domain.Payee;

import java.util.List;
import java.util.function.Predicate;

public class RsqlPredicateVisitor extends NoArgRSQLVisitorAdapter<Predicate<Payee>> {

    @Override
    public Predicate<Payee> visit(AndNode node) {
        return node.getChildren().stream()
                .map(child -> child.accept(this))
                .reduce(Predicate::and)
                .orElse(x -> true);
    }

    @Override
    public Predicate<Payee> visit(OrNode node) {
        return node.getChildren().stream()
                .map(child -> child.accept(this))
                .reduce(Predicate::or)
                .orElse(x -> false);
    }

    @Override
    public Predicate<Payee> visit(ComparisonNode node) {
        return payee -> {
            String attribute = node.getSelector();
            String value = node.getArguments().get(0);
            String fieldValue = getFieldValue(payee, attribute);

            if (node.getOperator().equals(RSQLOperators.EQUAL)) {
                return fieldValue != null && fieldValue.equals(value);
            }
            return false;
        };
    }

    private String getFieldValue(Payee payee, String attribute) {
        return switch (attribute) {
            case "name" -> payee.getName();
            case "iban" -> payee.getIban();
            case "id" -> payee.getId();
            default -> null;
        };
    }
}