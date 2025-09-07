package dexter.banking.booktransfers.infrastructure.adapter.out.stub.customer;

import dexter.banking.booktransfers.core.domain.payment.valueobject.RelId;
import dexter.banking.booktransfers.core.port.out.CustomerPort;
import org.springframework.stereotype.Component;

@Component
class CustomerAdapterStub implements CustomerPort {
    @Override
    public boolean isCustomerValid(RelId relId) {
        if (relId == null || relId.value() == null) {
            return false;
        }
        // As per the requirement, this stub provides a nuanced check.
        // It fails validation if the UUID string ends with 'X' (case-insensitive),
        // allowing us to test the failure path.
        return !relId.value().toString().toUpperCase().endsWith("X");
    }
}
