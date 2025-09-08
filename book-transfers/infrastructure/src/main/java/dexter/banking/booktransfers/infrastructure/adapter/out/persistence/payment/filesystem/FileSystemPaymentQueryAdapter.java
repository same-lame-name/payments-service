package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.payment.filesystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import dexter.banking.booktransfers.core.application.payment.query.PaymentView;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.port.in.payment.PaymentQueryUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
class FileSystemPaymentQueryAdapter implements PaymentQueryUseCase {

    private final Path storageDirectory;
    private final ObjectMapper objectMapper;

    FileSystemPaymentQueryAdapter(
            @Value("${payment.filesystem.storage-dir:./payments_data}") String storageDir,
            ObjectMapper objectMapper) {
        this.storageDirectory = Paths.get(storageDir);
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<PaymentView> findById(UUID transactionId) {
        return readMementoFromFile(storageDirectory.resolve(transactionId + ".json"))
                .map(this::toPaymentView);
    }

    @Override
    public List<PaymentView> findByReference(String transactionReference) {
        try (Stream<Path> paths = Files.walk(storageDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(this::readMementoFromFile)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(memento -> transactionReference.equals(memento.transactionReference()))
                    .map(this::toPaymentView)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Failed to scan storage directory for payments: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private Optional<Payment.PaymentMemento> readMementoFromFile(Path filePath) {
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            byte[] jsonBytes = Files.readAllBytes(filePath);
            return Optional.of(objectMapper.readValue(jsonBytes, Payment.PaymentMemento.class));
        } catch (IOException e) {
            System.err.println("Failed to read or parse payment file " + filePath + ": " + e.getMessage());
            return Optional.empty();
        }
    }

    private PaymentView toPaymentView(Payment.PaymentMemento memento) {
        return new PaymentView(
                memento.id(),
                memento.transactionReference(),
                memento.status(),
                memento.state()
        );
    }
}
