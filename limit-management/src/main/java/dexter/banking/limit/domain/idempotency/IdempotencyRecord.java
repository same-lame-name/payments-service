package dexter.banking.limit.domain.idempotency;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "idempotency_store")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdempotencyRecord implements Persistable<String> {

    @Id
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    private IdempotencyStatus status;

    @Lob
    @Convert(converter = PayloadEncryptionConverter.class)
    private String responsePayload;

    private String responseTypeAlias;

    private LocalDateTime createdAt;
    private LocalDateTime lockedAt;

    @Override
    public String getId() {
        return idempotencyKey;
    }

    @Override
    public boolean isNew() {
        // Always return true to force an INSERT statement.
        // If the key exists, the DB will throw a constraint violation
        return true;
    }
}