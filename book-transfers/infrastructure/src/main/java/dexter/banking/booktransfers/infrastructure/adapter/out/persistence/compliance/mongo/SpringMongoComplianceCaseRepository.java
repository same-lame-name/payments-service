package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.compliance.mongo;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface SpringMongoComplianceCaseRepository extends MongoRepository<ComplianceCaseDocument, String> {
    Optional<ComplianceCaseDocument> findByCaseId(UUID caseId);
    Optional<ComplianceCaseDocument> findByPaymentId(UUID paymentId);
}
