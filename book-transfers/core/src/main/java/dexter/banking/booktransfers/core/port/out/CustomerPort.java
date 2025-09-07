package dexter.banking.booktransfers.core.port.out;

import dexter.banking.booktransfers.core.domain.payment.valueobject.RelId;

public interface CustomerPort {
    boolean isCustomerValid(RelId relId);
}
