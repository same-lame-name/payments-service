package dexter.banking.booktransfers.infrastructure.adapter.out.persistence.payment.filesystem;

import dexter.banking.booktransfers.core.port.out.OrchestrationContextRepositoryPort;
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
class FileSystemOrchestrationContextRepositoryAdapter implements OrchestrationContextRepositoryPort {

    private final Path contextStorageDirectory;

    FileSystemOrchestrationContextRepositoryAdapter(
            @Value("${payment.filesystem.storage-dir:./payments_data}") String storageDir) {
        Path storageDirectory = Paths.get(storageDir);
        this.contextStorageDirectory = storageDirectory.resolve("context");
        try {
            Files.createDirectories(this.contextStorageDirectory);
        } catch (IOException e) {
            throw new RuntimeException("Could not create context storage directory", e);
        }
    }

    @Override
    public void save(UUID transactionId, byte[] contextData) {
        Path filePath = contextStorageDirectory.resolve(transactionId + ".json");
        try {
            Files.write(filePath, contextData);
        } catch (IOException e) {
            throw new RuntimeException("Failed to save orchestration context for " + transactionId, e);
        }
    }

    @Override
    public Optional<byte[]> findById(UUID transactionId) {
        Path filePath = contextStorageDirectory.resolve(transactionId + ".json");
        if (!Files.exists(filePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(filePath));
        } catch (IOException e) {
            System.err.println("Failed to read orchestration context for " + transactionId + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
