package dexter.banking.limit.service;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.gateway.RegulatorGateway;
import dexter.banking.limit.repository.PayeeRepository;
import dexter.banking.limit.repository.rsql.inmemory.InMemoryRsqlVisitor;
import dexter.banking.limit.repository.rsql.inmemory.InMemorySortBuilder;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import dexter.banking.limit.repository.rsql.common.SortConfig;
import dexter.banking.limit.repository.rsql.jpa.JpaSortTranslator;
import dexter.banking.limit.repository.rsql.jpa.JpaSpecificationVisitor;
import dexter.banking.limit.web.dto.PayeeDto;
import dexter.banking.limit.web.mapper.PayeeMapper;
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
public class PayeeQueryServiceImpl implements PayeeQueryService {

    private final PayeeRepository repository;
    private final RegulatorGateway regulatorGateway;
    private final FilterConfig<String> jpaFilterConfig;
    private final FilterConfig<Function<Payee, ?>> inMemoryFilterConfig;
    private final SortConfig<String> jpaSortConfig;
    private final SortConfig<Function<Payee, ? extends Comparable>> inMemorySortConfig;
    private final JpaSortTranslator sortTranslator;
    private final InMemorySortBuilder inMemorySortBuilder;
    private final PayeeMapper mapper;

    public PayeeQueryServiceImpl(PayeeRepository repository,
                                 RegulatorGateway regulatorGateway,
                                 @Qualifier("payeeJpaFilterConfig") FilterConfig<String> jpaFilterConfig,
                                 @Qualifier("payeeInMemoryFilterConfig") FilterConfig<Function<Payee, ?>> inMemoryFilterConfig,
                                 @Qualifier("payeeJpaSortConfig") SortConfig<String> jpaSortConfig,
                                 @Qualifier("payeeInMemorySortConfig") SortConfig<Function<Payee, ? extends Comparable>> inMemorySortConfig,
                                 JpaSortTranslator sortTranslator,
                                 InMemorySortBuilder inMemorySortBuilder,
                                 PayeeMapper mapper) {
        this.repository = repository;
        this.regulatorGateway = regulatorGateway;
        this.jpaFilterConfig = jpaFilterConfig;
        this.inMemoryFilterConfig = inMemoryFilterConfig;
        this.jpaSortConfig = jpaSortConfig;
        this.inMemorySortConfig = inMemorySortConfig;
        this.sortTranslator = sortTranslator;
        this.inMemorySortBuilder = inMemorySortBuilder;
        this.mapper = mapper;
    }

    @Override
    public Page<PayeeDto> list(Node filter, Sort sort, Pageable pageable) {
        Sort translatedSort = sortTranslator.translate(sort, jpaSortConfig);
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), translatedSort);
        
        Specification<Payee> spec = (filter != null) ? filter.accept(new JpaSpecificationVisitor<>(jpaFilterConfig)) : null;
        
        Page<Payee> entities = repository.findAll(spec, pageRequest);
        return entities.map(mapper::toDto);
    }

    @Override
    public Page<PayeeDto> listOnline(Node filter, Sort sort, Pageable pageable) {
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
        Page<Payee> entityPage = new PageImpl<>(pageContent, pageable, onlinePayees.size());
        
        return entityPage.map(mapper::toDto);
    }
    @Override
    public Optional<PayeeDto> getOne(String id) {
        return repository.findById(id).map(mapper::toDto);
    }
}