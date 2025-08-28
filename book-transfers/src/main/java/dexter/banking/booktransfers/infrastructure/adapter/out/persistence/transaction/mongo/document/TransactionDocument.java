package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.transaction.mongo.document;

import dexter.banking.booktransfers.core.domain.model.Status;
import dexter.banking.booktransfers.core.domain.model.TransactionState;
import dexter.banking.model.CreditCardBankingResponse;
import dexter.banking.model.DepositBankingResponse;
import dexter.banking.model.LimitManagementResponse;
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

    private DepositBankingResponse depositBankingResponse;

    private LimitManagementResponse limitManagementResponse;
    private CreditCardBankingResponse creditCardBankingResponse;

    private Status status = Status.NEW;
    private TransactionState state = TransactionState.NEW;
    // This infrastructure-specific field is persisted here, not in the domain model.
    private byte[] stateMachineContext;
}
