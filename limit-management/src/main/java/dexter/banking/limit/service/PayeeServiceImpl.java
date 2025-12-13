package dexter.banking.limit.service;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.gateway.RegulatorGateway;
import dexter.banking.limit.repository.PayeeRepository;
import dexter.banking.limit.repository.rsql.InMemoryRsqlVisitor;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import dexter.banking.limit.repository.rsql.jpa.JpaRsqlVisitor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
public class PayeeServiceImpl implements PayeeService {

    private final PayeeRepository repository;
    private final RegulatorGateway regulatorGateway;
    private final FilterConfig<String> jpaFilterConfig;
    private final FilterConfig<Function<Payee, ?>> inMemoryFilterConfig;

    public PayeeServiceImpl(PayeeRepository repository,
                            RegulatorGateway regulatorGateway,
                            @Qualifier("payeeJpaFilterConfig") FilterConfig<String> jpaFilterConfig,
                            @Qualifier("payeeInMemoryFilterConfig") FilterConfig<Function<Payee, ?>> inMemoryFilterConfig) {
        this.repository = repository;
        this.regulatorGateway = regulatorGateway;
        this.jpaFilterConfig = jpaFilterConfig;
        this.inMemoryFilterConfig = inMemoryFilterConfig;
    }

    @Override
    public Page<Payee> list(Node filter, Sort sort, Pageable pageable) {
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Specification<Payee> spec = (filter != null) ? filter.accept(new JpaRsqlVisitor<>(jpaFilterConfig)) : null;
        return repository.findAll(spec, pageRequest);
    }

    @Override
    public Page<Payee> listOnline(Node filter, Sort sort, Pageable pageable) {
        // 1. Fetch all from downstream
        List<Payee> onlinePayees = regulatorGateway.fetchPayees();

        // 2. Filter in-memory
        if (filter != null) {
            Predicate<Payee> predicate = filter.accept(new InMemoryRsqlVisitor<>(inMemoryFilterConfig));
            onlinePayees = onlinePayees.stream().filter(predicate).toList();
        }

        // 3. Sort in-memory
        if (sort.isSorted()) {
            Comparator<Payee> comparator = buildInMemoryComparator(sort);
            onlinePayees = onlinePayees.stream().sorted(comparator).toList();
        }

        // 4. Paginate in-memory
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), onlinePayees.size());

        List<Payee> pageContent = (start > onlinePayees.size()) ? List.of() : onlinePayees.subList(start, end);
        return new PageImpl<>(pageContent, pageable, onlinePayees.size());
    }

    @Override
    public Payee create(Payee payee) {
        return repository.save(payee);
    }

    @Override
    public Optional<Payee> getOne(String id) {
        return repository.findById(id);
    }

    private Comparator<Payee> buildInMemoryComparator(Sort sort) {
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