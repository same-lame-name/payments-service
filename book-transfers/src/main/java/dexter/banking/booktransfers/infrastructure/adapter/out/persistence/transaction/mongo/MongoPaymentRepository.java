package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.transaction.mongo;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.core.port.PaymentRepositoryPort;
import dexter.banking.booktransfers.infrastructure.adapter.out.persistence.transaction.mongo.document.TransactionDocument;
import dexter.banking.booktransfers.infrastructure.adapter.out.persistence.transaction.mongo.mapper.PersistenceMapper;
import dexter.banking.booktransfers.infrastructure.adapter.out.persistence.transaction.mongo.repository.SpringMongoTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component("mongodb")
@RequiredArgsConstructor
public class MongoPaymentRepository implements PaymentRepositoryPort {

    private final SpringMongoTransactionRepository repository;
    private final PersistenceMapper mapper;

    @Override
    public Payment save(Payment payment) {
        TransactionDocument document = mapper.toDocument(payment);
        TransactionDocument savedDocument = repository.save(document);
        return mapper.toDomain(savedDocument);
    }

    @Override
    public Payment update(Payment payment) {
        TransactionDocument doc = repository.findByTransactionId(payment.getId())
                .orElseGet(TransactionDocument::new);
        mapper.updateDocumentFromDomain(doc, payment);

        TransactionDocument savedDocument = repository.save(doc);
        return mapper.toDomain(savedDocument);
    }

    @Override
    public Optional<Payment> findById(UUID transactionId) {
        Optional<TransactionDocument> documentOptional = repository.findByTransactionId(transactionId);
        return documentOptional.map(mapper::toDomain);
    }
}
