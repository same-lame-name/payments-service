package dexter.banking.limit.pipeline.payee.strategy;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.pipeline.core.ExecutionStrategy;
import dexter.banking.limit.repository.PayeeRepository;
import dexter.banking.limit.web.dto.PayeeDto;
import dexter.banking.limit.web.mapper.PayeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class SepaPayeeStrategy implements ExecutionStrategy<PayeeDto, PayeeDto> {

    private final PayeeRepository repository;
    private final PayeeMapper mapper;

    @Override
    public Set<String> getSupportedIdentifiers() {
        return Set.of("SEPA");
    }

    @Override
    public PayeeDto execute(PayeeDto request) {
        Payee payee = mapper.toDomain(request);
        Payee saved = repository.save(payee);
        return mapper.toDto(saved);
    }
}