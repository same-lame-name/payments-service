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
            document.getDepositBankingResponse(),
            document.getLimitManagementResponse(),
            document.getCreditCardBankingResponse(),
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
        doc.setDepositBankingResponse(memento.depositBankingResponse());
        doc.setLimitManagementResponse(memento.limitManagementResponse());
        doc.setCreditCardBankingResponse(memento.creditCardBankingResponse());
    }
}
