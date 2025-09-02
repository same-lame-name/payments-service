package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.idempotency;

enum MongoIdempotencyStatus {
    STARTED,
    COMPLETED
}
