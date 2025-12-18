package dexter.banking.limit.service.idempotency;

import dexter.banking.limit.domain.idempotency.IdempotencyRecord;
import dexter.banking.limit.domain.idempotency.IdempotencyStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class IdempotencyService {

    private final IdempotencyRepository repository;
    private final TransactionTemplate transactionTemplate;

    public IdempotencyService(IdempotencyRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean tryAcquireLock(String key) {
        if (repository.existsById(key)) {
            return false;
        }

        // 2. Attempt atomic insert
        try {
            IdempotencyRecord record = new IdempotencyRecord(
                    key,
                    IdempotencyStatus.STARTED,
                    null,
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            );
            repository.saveAndFlush(record);
            return true;
        } catch (DataIntegrityViolationException e) {
            // Race condition detected. The key was inserted by another thread between our check and save.
            // The EntityManager has marked the transaction as rollback-only.
            // We explicitly acknowledge this to avoid UnexpectedRollbackException.
            return false;
        }
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> getOperationData(String key) {
        return repository.findById(key);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String key, String responsePayload, String responseTypeAlias) {
        IdempotencyRecord record = repository.findById(key)
                .orElseThrow(() -> new IllegalStateException("Cannot mark completed on a non-existent record: " + key));

        record.setStatus(IdempotencyStatus.COMPLETED);
        record.setResponsePayload(responsePayload);
        record.setResponseTypeAlias(responseTypeAlias);

        repository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseLock(String key) {
        repository.deleteById(key);
    }
}