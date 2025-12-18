package dexter.banking.limit.pipeline.payee.strategy;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.pipeline.core.BaseRequest;
import dexter.banking.limit.pipeline.core.ExecutionStrategy;
import dexter.banking.limit.repository.PayeeRepository;
import dexter.banking.limit.web.dto.PayeeDto;
import dexter.banking.limit.web.dto.UpdatePayeePatch;
import dexter.banking.limit.web.mapper.PayeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class UpdatePayeeStrategy implements ExecutionStrategy<UpdatePayeePatch, PayeeDto> {

    private final PayeeRepository repository;
    private final PayeeMapper mapper;

    @Override
    public Set<String> getSupportedIdentifiers() {
        return Set.of("PAYEE-UPDATE");
    }

    @Override
    @Transactional
    public PayeeDto execute(UpdatePayeePatch patch) {
        Payee entity = repository.findById(patch.getId())
                .orElseThrow(() -> new IllegalArgumentException("Payee not found: " + patch.getId()));

        PayeeDto businessDto = mapper.toDto(entity);
        mapper.updateDtoFromPatch(patch, businessDto);

        // --- SANE ZONE ---
        // Business logic would go here
        // --- SANE ZONE ---

        mapper.updateEntityFromPatch(patch, entity);
        Payee savedEntity = repository.save(entity);

        return mapper.toDto(savedEntity);
    }
}