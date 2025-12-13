package dexter.banking.limit.web;

import cz.jirutka.rsql.parser.ast.Node;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.gateway.RegulatorGateway;
import dexter.banking.limit.repository.PayeeRepository;
import dexter.banking.limit.repository.rsql.InMemoryRsqlVisitor;
import dexter.banking.limit.repository.rsql.common.FilterConfig;
import dexter.banking.limit.repository.rsql.jpa.JpaRsqlVisitor;
import dexter.banking.limit.web.dto.PayeeDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

@JsonApiController
@RequestMapping(path = "/api/v1/payees", produces = "application/vnd.api+json")
public class PayeeController {

    private final PayeeRepository repository;
    private final PayeeAssembler assembler;
    private final PagedResourcesAssembler<Payee> pagedResourcesAssembler;
    private final RegulatorGateway regulatorGateway;
    private final FilterConfig<String> jpaFilterConfig;
    private final FilterConfig<Function<Payee, ?>> inMemoryFilterConfig;

    public PayeeController(PayeeRepository repository,
                           PayeeAssembler assembler,
                           PagedResourcesAssembler<Payee> pagedResourcesAssembler,
                           RegulatorGateway regulatorGateway,
                           @Qualifier("payeeJpaFilterConfig") FilterConfig<String> jpaFilterConfig,
                           @Qualifier("payeeInMemoryFilterConfig") FilterConfig<Function<Payee, ?>> inMemoryFilterConfig) {
        this.repository = repository;
        this.assembler = assembler;
        this.pagedResourcesAssembler = pagedResourcesAssembler;
        this.regulatorGateway = regulatorGateway;
        this.jpaFilterConfig = jpaFilterConfig;
        this.inMemoryFilterConfig = inMemoryFilterConfig;
    }

    @GetMapping
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> list(
            @JsonApiFilter Node filter,
            @JsonApiSort Sort sort,
            @JsonApiPage Pageable pageable) {

        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Specification<Payee> spec = (filter != null) ? filter.accept(new JpaRsqlVisitor<>(jpaFilterConfig)) : null;
        Page<Payee> payees = repository.findAll(spec, pageRequest);

        PagedModel<EntityModel<PayeeDto>> pagedModel = pagedResourcesAssembler.toModel(payees, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @GetMapping("/online")
    public ResponseEntity<PagedModel<EntityModel<PayeeDto>>> onlineList(
            @JsonApiFilter Node filter,
            @JsonApiSort Sort sort,
            @JsonApiPage Pageable pageable) {

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
        Page<Payee> payeesPage = new PageImpl<>(pageContent, pageable, onlinePayees.size());

        PagedModel<EntityModel<PayeeDto>> pagedModel = pagedResourcesAssembler.toModel(payeesPage, assembler);
        return ResponseEntity.ok(pagedModel);
    }

    @PostMapping
    public ResponseEntity<EntityModel<PayeeDto>> create(@RequestBody EntityModel<PayeeDto> requestBody) {
        PayeeDto dto = requestBody.getContent();
        Payee domain = assembler.toDomain(dto);
        
        Payee saved = repository.save(domain);
        
        return ResponseEntity.created(URI.create("/api/v1/payees/" + saved.getId()))
                .body(assembler.toModel(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EntityModel<PayeeDto>> getOne(@PathVariable String id) {
        return repository.findById(id)
                .map(assembler::toModel)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
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