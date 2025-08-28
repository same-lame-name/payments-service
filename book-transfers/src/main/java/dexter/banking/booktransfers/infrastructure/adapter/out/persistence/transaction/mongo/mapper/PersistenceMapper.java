package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.transaction.mongo.mapper;

import dexter.banking.booktransfers.core.domain.model.Payment;
import dexter.banking.booktransfers.infrastructure.adapter.out.persistence.transaction.mongo.document.TransactionDocument;
import org.springframework.stereotype.Component;

@Component
public class PersistenceMapper {

    public Payment toDomain(TransactionDocument document) {
        if (document == null) {
            return null;
        }
        var memento = new Payment.PaymentMemento(
            document.getTransactionId(),
            document.getTransactionReference(),
            document.getDebitLegResult(),
            document.getLimitEarmarkResult(),
            document.getCreditLegResult(),
            document.getStatus(),
            document.getState()
        );
        return Payment.rehydrate(memento);
    }

    public TransactionDocument toDocument(Payment payment) {
        if (payment == null) {
            return null;
        }
        TransactionDocument document = new TransactionDocument();
        updateDocumentFromDomain(document, payment);
        return document;
    }

    public void updateDocumentFromDomain(TransactionDocument doc, Payment payment) {
        Payment.PaymentMemento memento = payment.getMemento();
        doc.setTransactionId(memento.id());
        doc.setTransactionReference(memento.transactionReference());
        doc.setStatus(memento.status());
        doc.setState(memento.state());
        doc.setDebitLegResult(memento.debitLegResult());
        doc.setLimitEarmarkResult(memento.limitEarmarkResult());
        doc.setCreditLegResult(memento.creditLegResult());
    }
}
