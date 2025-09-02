package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.idempotency.mongo;

enum MongoIdempotencyStatus {
    STARTED,
    COMPLETED
}
