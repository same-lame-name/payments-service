package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.payment.filesystem;

import com.fasterxml.jackson.databind.ObjectMapper;
import dexter.banking.booktransfers.core.domain.payment.Payment;
import dexter.banking.booktransfers.core.port.out.PaymentRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;

@Component
class FileSystemPaymentRepositoryAdapter implements PaymentRepositoryPort {

    private final Path storageDirectory;
    private final ObjectMapper objectMapper;

    FileSystemPaymentRepositoryAdapter(
            @Value("${payment.filesystem.storage-dir:./payments_data}") String storageDir,
            ObjectMapper objectMapper) {
        this.storageDirectory = Paths.get(storageDir);
        this.objectMapper = objectMapper;
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create storage directory", e);
        }
    }

    @Override
    public Payment save(Payment payment) {
        return persist(payment);
    }

    @Override
    public Payment update(Payment payment) {
        return persist(payment);
    }

    private Payment persist(Payment payment) {
        Payment.PaymentMemento memento = payment.getMemento();
        Path filePath = storageDirectory.resolve(memento.id() + ".json");
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(memento);
            Files.write(filePath, jsonBytes);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save payment " + memento.id(), e);
        }
        return payment;
    }

    @Override
    public Optional<Payment.PaymentMemento> findMementoById(UUID transactionId) {
        Path filePath = storageDirectory.resolve(transactionId + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            byte[] jsonBytes = Files.readAllBytes(filePath);
            Payment.PaymentMemento memento = objectMapper.readValue(jsonBytes, Payment.PaymentMemento.class);
            return Optional.of(memento);
        } catch (IOException e) {
            System.err.println("Failed to read or parse payment " + transactionId + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
