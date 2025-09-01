package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.idempotency;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Optional;

@Document(collection = "idempotency_records")
@Data
@NoArgsConstructor
public class MongoIdempotencyRecord {
    @Id
    private String idempotencyKey;
    private MongoIdempotencyStatus status;
    private String responseClassName;
    private String responseBody;

    @Indexed(expireAfterSeconds = 86400) // 24 hours
    private Instant createdAt;
    @SneakyThrows
    public Optional<Object> getResponse(ObjectMapper objectMapper) {
        if (this.status != MongoIdempotencyStatus.COMPLETED) {
            return Optional.empty();
        }
        if (this.responseBody == null || this.responseClassName.equals(Void.class.getName())) {
            return Optional.of(Void.TYPE);
        }
        Class<?> responseClass = Class.forName(this.responseClassName);
        return Optional.of(objectMapper.readValue(this.responseBody, responseClass));
    }
}
