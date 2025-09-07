package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.compliance.mongo;

import dexter.banking.booktransfers.core.domain.compliance.ComplianceCase;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface CompliancePersistenceMapper {

    @Mapping(target = "caseId", source = "id")
    @Mapping(target = "_id", ignore = true)
    ComplianceCaseDocument toDocument(ComplianceCase aggregate);

    default ComplianceCase toDomain(ComplianceCaseDocument document) {
        if (document == null) {
            return null;
        }
        // This default implementation is required to correctly use the aggregate's static factory method.
        return ComplianceCase.rehydrate(
                document.getCaseId(),
                document.getPaymentId(),
                document.getStatus(),
                document.getReason()
        );
    }

    @Mapping(target = "caseId", ignore = true)
    @Mapping(target = "paymentId", ignore = true)
    @Mapping(target = "_id", ignore = true)
    void updateDocument(@MappingTarget ComplianceCaseDocument doc, ComplianceCase aggregate);
}
