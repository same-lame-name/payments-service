package dexter.banking.limit.repository;

import dexter.banking.limit.domain.Payee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PayeeRepository extends JpaRepository<Payee, String>, JpaSpecificationExecutor<Payee> {
}