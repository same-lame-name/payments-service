package dexter.banking.limit.repository;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.domain.Payee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
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

    public List<Payee> findAll() {
        return List.copyOf(payees);
    }

    public Page<Payee> findAll(Node filterNode, Pageable pageable) {
        // 1. Filter
        List<Payee> filtered = payees;
        if (filterNode != null) {
            Predicate<Payee> predicate = filterNode.accept(new RsqlPredicateVisitor());
            filtered = payees.stream().filter(predicate).toList();
        }

        // 2. Sort
        if (pageable.getSort().isSorted()) {
            Comparator<Payee> comparator = buildComparator(pageable.getSort());
            if (comparator != null) {
                filtered = filtered.stream().sorted(comparator).toList();
            }
        }

        // 3. Paginate
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filtered.size());

        if (start > filtered.size()) {
            return new PageImpl<>(List.of(), pageable, filtered.size());
        }

        List<Payee> content = filtered.subList(start, end);
        return new PageImpl<>(content, pageable, filtered.size());
    }

    private Comparator<Payee> buildComparator(Sort sort) {
        Comparator<Payee> comparator = null;
        for (Sort.Order order : sort) {
            Comparator<Payee> current = switch (order.getProperty()) {
                case "name" -> Comparator.comparing(Payee::getName);
                case "iban" -> Comparator.comparing(Payee::getIban);
                case "id" -> Comparator.comparing(Payee::getId);
                default -> null;
            };

            if (current != null) {
                if (order.isDescending()) {
                    current = current.reversed();
                }
                comparator = (comparator == null) ? current : comparator.thenComparing(current);
            }
        }
        return comparator;
    }
}