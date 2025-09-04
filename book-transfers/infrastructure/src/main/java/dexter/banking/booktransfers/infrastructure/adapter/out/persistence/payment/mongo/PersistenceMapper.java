package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.payment.mongo;

import dexter.banking.booktransfers.core.domain.payment.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
interface PersistenceMapper {

    @Mapping(target = "id", source = "transactionId")
    Payment.PaymentMemento toMemento(TransactionDocument document);

    @Mapping(target = "transactionId", source = "memento.id")
    @Mapping(target = "transactionReference", source = "memento.transactionReference")
    @Mapping(target = "journeyName", source = "memento.journeyName")
    @Mapping(target = "status", source = "memento.status")
    @Mapping(target = "state", source = "memento.state")
    @Mapping(target = "debitLegResult", source = "memento.debitLegResult")
    @Mapping(target = "limitEarmarkResult", source = "memento.limitEarmarkResult")
    @Mapping(target = "creditLegResult", source = "memento.creditLegResult")
    @Mapping(target = "_id", ignore = true)
    @Mapping(target = "orchestrationContext", ignore = true)
    TransactionDocument toDocument(Payment payment);

    @Mapping(target = "transactionId", source = "memento.id")
    @Mapping(target = "transactionReference", source = "memento.transactionReference")
    @Mapping(target = "journeyName", source = "memento.journeyName")
    @Mapping(target = "status", source = "memento.status")
    @Mapping(target = "state", source = "memento.state")
    @Mapping(target = "debitLegResult", source = "memento.debitLegResult")
    @Mapping(target = "limitEarmarkResult", source = "memento.limitEarmarkResult")
    @Mapping(target = "creditLegResult", source = "memento.creditLegResult")
    @Mapping(target = "_id", ignore = true)
    @Mapping(target = "orchestrationContext", ignore = true)
    void updateDocumentFromDomain(@MappingTarget TransactionDocument doc, Payment payment);
}
