package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.compliance.mongo;

import dexter.banking.booktransfers.core.domain.compliance.ComplianceCase;
import dexter.banking.booktransfers.core.port.out.ComplianceCaseRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class MongoComplianceCaseRepository implements ComplianceCaseRepositoryPort {

    private final SpringMongoComplianceCaseRepository repository;
    private final CompliancePersistenceMapper mapper;

    @Override
    public void save(ComplianceCase complianceCase) {
        repository.findByCaseId(complianceCase.getId())
                .ifPresentOrElse(
                        doc -> {
                            mapper.updateDocument(doc, complianceCase);
                            repository.save(doc);
                        },
                        () -> {
                            ComplianceCaseDocument doc = mapper.toDocument(complianceCase);
                            repository.save(doc);
                        }
                );
    }

    @Override
    public Optional<ComplianceCase> findById(UUID complianceCaseId) {
        return repository.findByCaseId(complianceCaseId)
                .map(mapper::toDomain);
    }
}
