package dexter.banking.limit.util;

import dexter.banking.limit.domain.Address;
import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.repository.PayeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
public class DataLoader implements CommandLineRunner {

    private final PayeeRepository payeeRepository;

    public DataLoader(PayeeRepository payeeRepository) {
        this.payeeRepository = payeeRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Initialize with dummy data for pagination testing
        IntStream.rangeClosed(1, 55).forEach(i -> {
            Address address = new Address(getCity(i), "Zip" + i);
            LocalDate dob = LocalDate.of(1980 + (i % 20), 1 + (i % 12), 1 + (i % 28));
            OffsetDateTime createdAt = OffsetDateTime.of(2023, 10, 27, 10, i, 0, 0, ZoneOffset.UTC);

            payeeRepository.save(new Payee(
                    null, // Let JPA generate the ID
                    "Payee " + i,
                    "DE" + (10000000 + i),
                    dob,
                    createdAt,
                    address
            ));
        });
    }

    private String getCity(int i) {
        return switch (i % 4) {
            case 0 -> "London";
            case 1 -> "New York";
            case 2 -> "Paris";
            default -> "Tokyo";
        };
    }
}