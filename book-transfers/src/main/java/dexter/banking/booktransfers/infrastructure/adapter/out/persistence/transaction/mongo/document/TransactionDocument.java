package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.transaction.mongo.document;

import dexter.banking.booktransfers.core.domain.model.PaymentState;
import dexter.banking.booktransfers.core.domain.model.Status;
import dexter.banking.booktransfers.core.domain.model.results.CreditLegResult;
import dexter.banking.booktransfers.core.domain.model.results.DebitLegResult;
import dexter.banking.booktransfers.core.domain.model.results.LimitEarmarkResult;
import lombok.Getter;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Setter
@Getter
@Document(collection = "transaction_info")
public class TransactionDocument {

    @Id
    private ObjectId _id;
    @Indexed(unique = true)
    private UUID transactionId;
    private String transactionReference;
    private String journeyName; // Persisted context for rehydration

    // The document now stores the pure, lean domain value objects.
    private DebitLegResult debitLegResult;
    private LimitEarmarkResult limitEarmarkResult;
    private CreditLegResult creditLegResult;
    private Status status = Status.NEW;
    private PaymentState state = PaymentState.NEW;

    // This infrastructure-specific field is persisted here, not in the domain model.
    private byte[] orchestrationContext;
}
