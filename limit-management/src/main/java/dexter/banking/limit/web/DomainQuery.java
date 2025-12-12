package dexter.banking.limit.web;

import cz.jirutka.rsql.parser.ast.Node;
import org.springframework.data.domain.Pageable;

public record DomainQuery(Node filterNode, Pageable pageable) {

    public static DomainQuery empty() {
        return new DomainQuery(null, Pageable.unpaged());
    }
}