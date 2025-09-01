package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.payment;

import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
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
        repository.save(document);
        // We return the original aggregate as its state is authoritative.
        // The saved document is just a persistence artifact.
        return payment;
    }

    @Override
    public Payment update(Payment payment) {
        TransactionDocument doc = repository.findByTransactionId(payment.getId())
                .orElseThrow(() -> new IllegalStateException("Attempted to update a non-existent payment: " + payment.getId()));
        mapper.updateDocumentFromDomain(doc, payment);
        repository.save(doc);
        // Return original aggregate, which holds the live BusinessPolicy and correct state.
        return payment;
    }

    @Override
    public Optional<Payment.PaymentMemento> findMementoById(UUID transactionId) {
        return repository.findByTransactionId(transactionId)
                .map(mapper::toMemento);
    }
}
