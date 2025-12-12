package dexter.banking.limit.repository;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.web.DomainQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import java.util.stream.IntStream;

@Repository
public class PayeeRepository {

    private final List<Payee> payees = new CopyOnWriteArrayList<>();

    public PayeeRepository() {
        // Initialize with dummy data for pagination testing
        IntStream.rangeClosed(1, 55).forEach(i -> {
            payees.add(new Payee(
                    UUID.randomUUID().toString(),
                    "Payee " + i,
                    "DE" + (10000000 + i)
            ));
        });
    }

    public Payee save(Payee payee) {
        if (payee.getId() == null) {
            payee.setId(UUID.randomUUID().toString());
        }
        payees.add(payee);
        return payee;
    }

    public Page<Payee> findAll(DomainQuery query) {
        Pageable pageable = query.pageable();
        
        // 1. Filter
        List<Payee> filtered = payees;
        if (query.filterNode() != null) {
            Predicate<Payee> predicate = query.filterNode().accept(new RsqlPredicateVisitor());
            filtered = payees.stream().filter(predicate).toList();
        }

        // 2. Paginate
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());

        if (start > filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }

        List<Payee> content = filtered.subList(start, end);
        return new PageImpl<>(content, pageable, filtered.size());
    }
}