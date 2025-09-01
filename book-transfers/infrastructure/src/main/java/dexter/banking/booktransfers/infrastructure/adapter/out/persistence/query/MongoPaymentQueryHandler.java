package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.query;

import dexter.banking.booktransfers.core.application.payment.query.PaymentView;
import dexter.banking.booktransfers.core.port.in.payment.PaymentQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Query Adapter for payments.
 * This is a highly-optimized implementation that uses MongoDB projections to fetch
 * only the necessary data, mapping it directly to the lean PaymentView read model.
 * It completely bypasses the rich domain model for reads.
 */
@Component
@RequiredArgsConstructor
public class MongoPaymentQueryHandler implements PaymentQueryUseCase {

    private final MongoTemplate mongoTemplate;
    private static final String TRANSACTION_COLLECTION = "transaction_info";

    @Override
    public Optional<PaymentView> findById(UUID transactionId) {
        Query query = new Query(Criteria.where("transactionId").is(transactionId));
        addProjection(query);

        PaymentView result = mongoTemplate.findOne(query, PaymentView.class, TRANSACTION_COLLECTION);
        return Optional.ofNullable(result);
    }

    @Override
    public List<PaymentView> findByReference(String transactionReference) {
        Query query = new Query(Criteria.where("transactionReference").is(transactionReference));
        addProjection(query);
        return mongoTemplate.find(query, PaymentView.class, TRANSACTION_COLLECTION);
    }

    private void addProjection(Query query) {
        // CRITICAL: Use projection to fetch ONLY the fields we need.
        // This is the key to a performant query stack.
        query.fields()
                .include("transactionId")
                .include("transactionReference")
                .include("status")
                .include("state");
    }
}
