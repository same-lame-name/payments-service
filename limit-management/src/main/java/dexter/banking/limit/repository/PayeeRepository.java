package dexter.banking.limit.repository;

import dexter.banking.limit.domain.Payee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

@Repository
public class PayeeRepository {

    private final List<Payee> payees = new CopyOnWriteArrayList<>();

    public PayeeRepository() {
        // Initialize with dummy data for pagination testing
        IntStream.rangeClosed(1, 55).forEach(i -> {
            payees.add(new Payee(
                    UUID.randomUUID().toString(),
                    "Payee " + i,
                    "DE" + (10000000 + i)
            ));
        });
    }

    public Payee save(Payee payee) {
        if (payee.getId() == null) {
            payee.setId(UUID.randomUUID().toString());
        }
        payees.add(payee);
        return payee;
    }

    public Page<Payee> findAll(Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), payees.size());

        if (start > payees.size()) {
            return new PageImpl<>(List.of(), pageable, payees.size());
        }

        List<Payee> content = payees.subList(start, end);
        return new PageImpl<>(content, pageable, payees.size());
    }
}