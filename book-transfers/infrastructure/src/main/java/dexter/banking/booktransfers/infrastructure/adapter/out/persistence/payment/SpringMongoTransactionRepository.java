package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.payment;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * The internal Spring Data MongoDB repository interface.
 * This is an implementation detail of the persistence adapter and is not exposed
 * to the application core. It works with the TransactionDocument.
 */
@Repository
interface SpringMongoTransactionRepository extends MongoRepository<TransactionDocument, String> {
    Optional<TransactionDocument> findByTransactionId(UUID transactionId);
}


