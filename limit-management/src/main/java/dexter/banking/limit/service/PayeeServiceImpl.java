package dexter.banking.limit.service;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.gateway.RegulatorGateway;
import dexter.banking.limit.repository.PayeeRepository;
import dexter.banking.limit.repository.rsql.inmemory.InMemoryRsqlVisitor;
import dexter.banking.limit.repository.rsql.inmemory.InMemorySortBuilder;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import dexter.banking.limit.repository.rsql.common.SortConfig;
import dexter.banking.limit.repository.rsql.jpa.SortTranslator;
import dexter.banking.limit.repository.rsql.jpa.builder.RSQLSpecificationBuilder;
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
    private final FilterConfig<Function<Payee, ?>> inMemoryFilterConfig;
    private final SortConfig<String> jpaSortConfig;
    private final SortConfig<Function<Payee, ? extends Comparable>> inMemorySortConfig;
    private final SortTranslator sortTranslator;
    private final InMemorySortBuilder inMemorySortBuilder;

    public PayeeServiceImpl(PayeeRepository repository,
                            RegulatorGateway regulatorGateway,
                            @Qualifier("payeeInMemoryFilterConfig") FilterConfig<Function<Payee, ?>> inMemoryFilterConfig,
                            @Qualifier("payeeJpaSortConfig") SortConfig<String> jpaSortConfig,
                            @Qualifier("payeeInMemorySortConfig") SortConfig<Function<Payee, ? extends Comparable>> inMemorySortConfig,
                            SortTranslator sortTranslator,
                            InMemorySortBuilder inMemorySortBuilder) {
        this.repository = repository;
        this.regulatorGateway = regulatorGateway;
        this.inMemoryFilterConfig = inMemoryFilterConfig;
        this.jpaSortConfig = jpaSortConfig;
        this.inMemorySortConfig = inMemorySortConfig;
        this.sortTranslator = sortTranslator;
        this.inMemorySortBuilder = inMemorySortBuilder;
    }

    @Override
    public Page<Payee> list(Node filter, Sort sort, Pageable pageable) {
        Sort translatedSort = sortTranslator.translate(sort, jpaSortConfig);
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), translatedSort);
        
        Specification<Payee> spec = (filter != null) ? new RSQLSpecificationBuilder<Payee>().build(filter) : null;
        
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
            Comparator<Payee> comparator = inMemorySortBuilder.build(sort, inMemorySortConfig);
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
}