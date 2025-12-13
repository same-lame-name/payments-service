package dexter.banking.limit.util;

import dexter.banking.limit.domain.Payee;
import dexter.banking.limit.repository.PayeeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

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
            payeeRepository.save(new Payee(
                    UUID.randomUUID().toString(),
                    "Payee " + i,
                    "DE" + (10000000 + i)
            ));
        });
    }
}