package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.compliance.mongo;

import dexter.banking.booktransfers.core.domain.compliance.ComplianceCase;
import org.springframework.stereotype.Component;

@Component
class CompliancePersistenceMapper {

    public ComplianceCaseDocument toDocument(ComplianceCase aggregate) {
        if (aggregate == null) {
            return null;
        }
        ComplianceCaseDocument document = new ComplianceCaseDocument();
        document.setCaseId(aggregate.getId());
        document.setPaymentId(aggregate.getPaymentId());
        document.setStatus(aggregate.getStatus());
        document.setReason(aggregate.getReason());
        return document;
    }

    public ComplianceCase toDomain(ComplianceCaseDocument document) {
        if (document == null) {
            return null;
        }
        return ComplianceCase.rehydrate(
                document.getCaseId(),
                document.getPaymentId(),
                document.getStatus(),
                document.getReason()
        );
    }

    public void updateDocument(ComplianceCaseDocument doc, ComplianceCase aggregate) {
        doc.setStatus(aggregate.getStatus());
        doc.setReason(aggregate.getReason());
    }
}
