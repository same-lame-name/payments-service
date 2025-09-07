package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.compliance.mongo;

import dexter.banking.booktransfers.core.domain.compliance.ComplianceStatus;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Data
@Document(collection = "compliance_cases")
class ComplianceCaseDocument {
    @Id
    private ObjectId _id;

    @Indexed(unique = true)
    private UUID caseId;

    private UUID paymentId;
    private ComplianceStatus status;
    private String reason;
}
